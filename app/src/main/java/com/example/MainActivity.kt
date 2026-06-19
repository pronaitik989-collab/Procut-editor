package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.editor.CapCutEditorContent
import com.example.editor.EditorViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Programmatic initialization of Firebase
    try {
      if (FirebaseApp.getApps(this).isEmpty()) {
        val options = FirebaseOptions.Builder()
          .setApiKey(BuildConfig.FIREBASE_API_KEY)
          .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
          .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
          .build()
        FirebaseApp.initializeApp(this, options)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    setContent {
      MyApplicationTheme(darkTheme = true) {
        val viewModel = remember { EditorViewModel() }
        CapCutEditorContent(viewModel = viewModel)
      }
    }
  }
}
