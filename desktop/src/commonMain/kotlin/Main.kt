import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kiryantsev.commonui.UIShow

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NotaMusic",
    ) {
        UIShow()
    }
}
