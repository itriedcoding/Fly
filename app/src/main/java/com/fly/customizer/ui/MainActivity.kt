package com.fly.customizer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.fly.customizer.R
import com.fly.customizer.core.Prefs
import com.fly.customizer.core.ThemeController
import com.fly.customizer.databinding.ActivityMainBinding
import com.fly.customizer.ui.home.HomeFragment
import com.fly.customizer.ui.icons.IconsFragment
import com.fly.customizer.ui.settings.SettingsFragment
import com.fly.customizer.ui.themes.ThemesFragment
import com.fly.customizer.ui.wallpapers.WallpapersFragment
import com.fly.customizer.ui.widgets.WidgetsFragment

/**
 * Single-activity host: toolbar, tab container and bottom navigation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        ThemeController.apply(this, prefs)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.inflateMenu(R.menu.menu_toolbar)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) {
                openScreen(SettingsFragment())
                true
            } else {
                false
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            prefs.lastTab = item.order
            showTab(item.itemId)
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = tabIdAt(prefs.lastTab)
        }
    }

    /** Switches to a bottom navigation tab programmatically. */
    fun selectTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    /** Pushes a screen on top of the current tab (back returns to the tab). */
    fun openScreen(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(SCREEN_TAG)
            .commit()
    }

    private fun tabIdAt(index: Int): Int = when (index) {
        1 -> R.id.nav_wallpapers
        2 -> R.id.nav_themes
        3 -> R.id.nav_icons
        4 -> R.id.nav_widgets
        else -> R.id.nav_home
    }

    private fun showTab(itemId: Int) {
        val fragment: Fragment = when (itemId) {
            R.id.nav_wallpapers -> WallpapersFragment()
            R.id.nav_themes -> ThemesFragment()
            R.id.nav_icons -> IconsFragment()
            R.id.nav_widgets -> WidgetsFragment()
            else -> HomeFragment()
        }
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    private companion object {
        const val SCREEN_TAG = "fly_screen"
    }
}
