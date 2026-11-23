package com.keiranhaas.auralarc.ui

import androidx.navigation.NavHostController

/**
 * Pops the back stack, but never removes the last screen.
 *
 * Rapid back taps can arrive while a screen is still animating out.
 * A plain popBackStack() would then remove the home screen too and
 * leave an empty (black) NavHost.
 */
fun NavHostController.safePopBackStack(): Boolean =
    if (previousBackStackEntry != null) {
        popBackStack()
    } else {
        false
    }