package app.luisramos.ler.ui.screen

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils.loadAnimation
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import app.luisramos.ler.R
import java.util.*

abstract class NavigatingActivity : AppCompatActivity() {

    companion object {
        val NO_MENU: (Menu) -> Unit = {}
    }

    lateinit var backstack: ArrayList<BackstackFrame>
    private lateinit var currentScreen: Screen

    private lateinit var container: ViewGroup
    private lateinit var currentView: View

    private val viewModel: NavigatingActivityViewModel by viewModels()

    var onCreateOptionsMenu = NO_MENU

    fun installNavigation(
        savedInstanceState: Bundle?,
        container: ViewGroup
    ) {
        this.container = container

        if (savedInstanceState == null) {
            backstack = ArrayList()
            currentScreen = if (intent.hasExtra("screens")) {
                @Suppress("UNCHECKED_CAST")
                val screens = intent.getSerializableExtra("screens") as List<Screen>
                screens.dropLast(1)
                    .forEach { screen ->
                        backstack.add(BackstackFrame(screen))
                    }
                screens.last()
            } else {
                getLauncherScreen()
            }
        } else {
            currentScreen = savedInstanceState.getSerializable("currentScreen") as Screen
            @Suppress("UNCHECKED_CAST")
            backstack =
                savedInstanceState.getParcelableArrayList<Parcelable>("backstack") as ArrayList<BackstackFrame>
        }
        currentView = currentScreen.createView(container)
        container.addView(currentView)
        screenUpdated()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra("screens")) {
            @Suppress("UNCHECKED_CAST")
            val screens = intent.getSerializableExtra("screens") as List<Screen>
            goTo(intent.getSerializableExtra("screen") as Screen)
            backstack.clear()
            screens.dropLast(1)
                .forEach { screen ->
                    backstack.add(BackstackFrame(screen))
                }
        }
    }

    open fun getLauncherScreen(): Screen {
        TODO("Launcher activities should override getLauncherScreen()")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("currentScreen", currentScreen)
        outState.putParcelableArrayList("backstack", backstack)
    }

    override fun onBackPressed() {
        if (backstack.size > 0) {
            goBack()
            return
        }
        super.onBackPressed()
    }

    fun resetTo(screen: Screen) {
        onCreateOptionsMenu = NO_MENU

        currentView.startAnimation(loadAnimation(this, R.anim.screen_exit_alpha))
        container.removeView(currentView)
        currentView.notifyScreenExiting()

        backstack.forEach { frame -> viewModel.clearStore(frame.screen) }
        backstack.clear()

        currentScreen = screen
        currentView = currentScreen.createView(container)
        currentView.startAnimation(loadAnimation(this, R.anim.screen_enter_alpha))
        container.addView(currentView)

        screenUpdated()
    }

    fun goTo(screen: Screen) {
        onCreateOptionsMenu = NO_MENU

        currentView.startAnimation(loadAnimation(this, R.anim.screen_exit_forward))
        container.removeView(currentView)
        currentView.notifyScreenExiting()
        val backstackFrame = BackstackFrame(currentScreen, currentView)
        backstack.add(backstackFrame)

        currentScreen = screen
        currentView = currentScreen.createView(container)
        currentView.startAnimation(loadAnimation(this, R.anim.screen_enter_forward))
        container.addView(currentView)

        screenUpdated()
    }

    fun refreshCurrentScreen() {
        onCreateOptionsMenu = NO_MENU
        container.removeView(currentView)
        currentView.notifyScreenExiting()
        currentView = currentScreen.createView(container)
        container.addView(currentView)

        screenUpdated()
    }

    fun goBack() {
        onCreateOptionsMenu = NO_MENU

        currentView.startAnimation(loadAnimation(this, R.anim.exit_backward))
        container.removeView(currentView)
        currentView.notifyScreenExiting()
        viewModel.clearStore(currentScreen)

        val latest = backstack.removeAt(backstack.size - 1)
        currentScreen = latest.screen
        currentView = currentScreen.createView(container)
        currentView.startAnimation(loadAnimation(this, R.anim.enter_backward))
        container.addView(currentView, 0)
        latest.restore(currentView)

        screenUpdated()
    }

    fun getViewModelStoreForScreen(screen: Screen) =
        viewModel.getStore(screen)

    private fun screenUpdated() {
        invalidateOptionsMenu()
        val actionBar = supportActionBar ?: return
        val homeEnabled = backstack.size > 0
        actionBar.setDisplayHomeAsUpEnabled(homeEnabled)
        actionBar.setHomeButtonEnabled(homeEnabled)
        onNewScreen(currentScreen)
    }

    protected open fun onNewScreen(screen: Screen) {
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        onCreateOptionsMenu.invoke(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                goBack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onDestroy() {
        super.onDestroy()
        currentView.notifyScreenExiting()
    }
}
