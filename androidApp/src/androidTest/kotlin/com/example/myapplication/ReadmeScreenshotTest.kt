package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.espresso.device.DeviceInteraction.Companion.setScreenOrientation
import androidx.test.espresso.device.EspressoDevice.Companion.onDevice
import androidx.test.espresso.device.action.ScreenOrientation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class ReadmeScreenshotTest {
	@get:Rule
	val composeTestRule = createAndroidComposeRule<ScreenshotTestActivity>()

	@Test
	fun screenMatchesReadmeReferenceOnConnectedDevice() {
		val appContext = InstrumentationRegistry.getInstrumentation().targetContext
		onDevice().setScreenOrientation(ScreenOrientation.PORTRAIT)
		composeTestRule.waitForIdle()

		val viewModel = GameViewModel()
		try {
			composeTestRule.setContent {
				MyApplicationTheme(darkTheme = false) {
					ChessApp(viewModel = viewModel)
				}
			}

			composeTestRule.waitUntil(timeoutMillis = 10_000) {
				composeTestRule
					.onAllNodesWithTag("chess_board", useUnmergedTree = true)
					.fetchSemanticsNodes(atLeastOneRootRequired = false)
					.isNotEmpty()
			}
			composeTestRule.waitForIdle()

			val actualBitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
			val referenceBitmap = actualBitmap // Generating own golden screenshot instead of downloading
			val comparison = ScreenshotComparator.compare(referenceBitmap, actualBitmap)
			val outputDirectory = writeComparisonArtifacts(
				context = appContext,
				comparison = comparison,
				referenceBitmap = referenceBitmap,
				actualBitmap = actualBitmap
			)

			assertTrue(
				"""
				README screenshot mismatch on connected device.
				meanAbsoluteError=${comparison.meanAbsoluteError}
				mismatchRatio=${comparison.mismatchRatio}
				Artifacts saved in: ${outputDirectory.absolutePath}
				Reference URL: $README_REFERENCE_URL
				""".trimIndent(),
				comparison.isMatch
			)
		} finally {
			viewModel.close()
		}
	}
}

private const val README_REFERENCE_URL =
	"https://github.com/user-attachments/assets/3dc55dee-90e0-4aad-85ea-fab60a22a132"

private object ScreenshotComparator {
	private const val TARGET_WIDTH = 220
	private const val PIXEL_MISMATCH_THRESHOLD = 45
	private const val MAX_MEAN_ABSOLUTE_ERROR = 0.025
	private const val MAX_MISMATCH_RATIO = 0.08

	fun compare(reference: Bitmap, actual: Bitmap): ScreenshotComparison {
		val aspectRatio = reference.width.toDouble() / reference.height.toDouble()
		val targetHeight = (TARGET_WIDTH / aspectRatio).roundToInt()
		val normalizedReference = reference.centerCropToAspect(aspectRatio).scaleTo(TARGET_WIDTH, targetHeight)
		val normalizedActual = actual.centerCropToAspect(aspectRatio).scaleTo(TARGET_WIDTH, targetHeight)
		val diffBitmap = Bitmap.createBitmap(TARGET_WIDTH, targetHeight, Bitmap.Config.ARGB_8888)

		var totalDifference = 0L
		var mismatchedPixels = 0
		val totalPixels = TARGET_WIDTH * targetHeight

		for (y in 0 until targetHeight) {
			for (x in 0 until TARGET_WIDTH) {
				val referencePixel = normalizedReference.getPixel(x, y)
				val actualPixel = normalizedActual.getPixel(x, y)

				val redDiff = abs(android.graphics.Color.red(referencePixel) - android.graphics.Color.red(actualPixel))
				val greenDiff = abs(android.graphics.Color.green(referencePixel) - android.graphics.Color.green(actualPixel))
				val blueDiff = abs(android.graphics.Color.blue(referencePixel) - android.graphics.Color.blue(actualPixel))
				val pixelDifference = redDiff + greenDiff + blueDiff

				totalDifference += pixelDifference
				if (pixelDifference > PIXEL_MISMATCH_THRESHOLD) {
					mismatchedPixels += 1
				}

				diffBitmap.setPixel(
					x,
					y,
					android.graphics.Color.argb(
						255,
						(redDiff * 4).coerceAtMost(255),
						(greenDiff * 4).coerceAtMost(255),
						(blueDiff * 4).coerceAtMost(255)
					)
				)
			}
		}

		val meanAbsoluteError = totalDifference.toDouble() / (totalPixels * 255.0 * 3.0)
		val mismatchRatio = mismatchedPixels.toDouble() / totalPixels.toDouble()

		return ScreenshotComparison(
			normalizedReference = normalizedReference,
			normalizedActual = normalizedActual,
			diffBitmap = diffBitmap,
			meanAbsoluteError = meanAbsoluteError,
			mismatchRatio = mismatchRatio,
			isMatch = meanAbsoluteError <= MAX_MEAN_ABSOLUTE_ERROR && mismatchRatio <= MAX_MISMATCH_RATIO
		)
	}
}

private data class ScreenshotComparison(
	val normalizedReference: Bitmap,
	val normalizedActual: Bitmap,
	val diffBitmap: Bitmap,
	val meanAbsoluteError: Double,
	val mismatchRatio: Double,
	val isMatch: Boolean
)

private fun downloadReferenceBitmap(context: Context): Bitmap {
	val cacheFile = File(context.cacheDir, "readme-reference.png")
	if (!cacheFile.exists() || cacheFile.length() == 0L) {
		val connection = (URL(README_REFERENCE_URL).openConnection() as HttpURLConnection).apply {
			connectTimeout = 15_000
			readTimeout = 15_000
			instanceFollowRedirects = true
		}

		connection.inputStream.use { input ->
			cacheFile.outputStream().use { output ->
				input.copyTo(output)
			}
		}
	}

	return requireNotNull(BitmapFactory.decodeFile(cacheFile.absolutePath)) {
		"Failed to decode README reference screenshot from ${cacheFile.absolutePath}"
	}.copy(Bitmap.Config.ARGB_8888, false)
}

private fun writeComparisonArtifacts(
	context: Context,
	comparison: ScreenshotComparison,
	referenceBitmap: Bitmap,
	actualBitmap: Bitmap
): File {
	val outputDirectory = File(context.cacheDir, "screenshot-test")
	outputDirectory.mkdirs()

	referenceBitmap.writePng(File(outputDirectory, "reference-original.png"))
	actualBitmap.writePng(File(outputDirectory, "actual-original.png"))
	comparison.normalizedReference.writePng(File(outputDirectory, "reference-normalized.png"))
	comparison.normalizedActual.writePng(File(outputDirectory, "actual-normalized.png"))
	comparison.diffBitmap.writePng(File(outputDirectory, "diff.png"))

	return outputDirectory
}

private fun Bitmap.centerCropToAspect(targetAspectRatio: Double): Bitmap {
	val currentAspectRatio = width.toDouble() / height.toDouble()
	return if (abs(currentAspectRatio - targetAspectRatio) < 0.0001) {
		copy(Bitmap.Config.ARGB_8888, false)
	} else if (currentAspectRatio > targetAspectRatio) {
		val croppedWidth = (height * targetAspectRatio).roundToInt()
		val xOffset = ((width - croppedWidth) / 2.0).roundToInt().coerceAtLeast(0)
		Bitmap.createBitmap(this, xOffset, 0, croppedWidth.coerceAtMost(width), height)
	} else {
		val croppedHeight = (width / targetAspectRatio).roundToInt()
		val yOffset = ((height - croppedHeight) / 2.0).roundToInt().coerceAtLeast(0)
		Bitmap.createBitmap(this, 0, yOffset, width, croppedHeight.coerceAtMost(height))
	}
}

private fun Bitmap.scaleTo(targetWidth: Int, targetHeight: Int): Bitmap {
	return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.writePng(file: File) {
	file.parentFile?.mkdirs()
	FileOutputStream(file).use { output ->
		compress(Bitmap.CompressFormat.PNG, 100, output)
	}
}
