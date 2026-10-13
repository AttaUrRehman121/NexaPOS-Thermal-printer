package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.printer.EscPosBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NexaPOS", appName)
  }

  @Test
  fun `esc pos builder produces valid command bytes`() {
    val builder = EscPosBuilder()
    builder.reset()
      .alignCenter()
      .bold(true)
      .line("STORE RECEIPT")
      .twoColumn("Item", "10.00", 32)
      .cutPaper()

    val bytes = builder.build()
    assertTrue(bytes.isNotEmpty())
    // Starts with ESC @ (0x1B, 0x40)
    assertEquals(0x1B.toByte(), bytes[0])
    assertEquals(0x40.toByte(), bytes[1])
  }
}
