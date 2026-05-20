package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Speedy Report Bot", appName)
  }

  @Test
  fun `test MainViewModel instantiation`() {
    try {
      val app = ApplicationProvider.getApplicationContext<Application>()
      val viewModel = MainViewModel(app)
      assertNotNull(viewModel)
    } catch (e: Throwable) {
      e.printStackTrace()
      throw e
    }
  }
}
