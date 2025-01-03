package com.rk.xededitor.ui.screens.settings.git

import android.app.Activity
import android.content.Context
import android.util.Patterns
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.resources.strings
import com.rk.xededitor.rkUtils
import com.rk.xededitor.rkUtils.toastIt
import com.rk.xededitor.ui.components.InputDialog
import com.rk.xededitor.ui.components.SettingsToggle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.robok.engine.core.components.compose.preferences.base.PreferenceGroup
import org.robok.engine.core.components.compose.preferences.base.PreferenceLayout

@Composable
fun SettingsGitScreen() {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity

    var username by remember { mutableStateOf("root") }
    var email by remember { mutableStateOf("example@mail.com") }
    var token by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var showEmailDialog by remember { mutableStateOf(false) }
    var showUserNameDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }

    var inputEmail by remember { mutableStateOf("") }
    var inputUserName by remember { mutableStateOf("") }
    var inputToken by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val gitConfig = loadGitConfig(context)
        username = gitConfig.first
        email = gitConfig.second
        token = getToken(context)

        inputEmail = gitConfig.second
        inputUserName = gitConfig.first
        inputToken = token

        isLoading = false
    }

    PreferenceLayout(label = stringResource(id = strings.git), backArrowVisible = true) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            PreferenceGroup {
                SettingsToggle(label = "UserName",
                    description = username,
                    showSwitch = false,
                    sideEffect = {
                        showUserNameDialog = true
                    })

                SettingsToggle(label = "Email",
                    description = email,
                    showSwitch = false,
                    sideEffect = {
                        showEmailDialog = true
                    })

                SettingsToggle(label = "Github Token",
                    description = "Github Token",
                    showSwitch = false,
                    sideEffect = {
                        showTokenDialog = true
                    })


            }

            if (showEmailDialog) {
                InputDialog(
                    title = "Email",
                    inputLabel = "example@email.com",
                    inputValue = inputEmail,
                    onInputValueChange = { text ->
                        inputEmail = text
                    },
                    onConfirm = {
                        runCatching {
                            if (isValidEmail(inputEmail)) {
                                updateConfig(context, username, inputEmail)
                                email = inputEmail
                            } else {
                                inputEmail = email
                                rkUtils.toast("Invalid Email")
                            }
                        }.onFailure { rkUtils.toast(it.message) }
                        showEmailDialog = false
                    },
                    onDismiss = {
                        showEmailDialog = false
                        inputEmail = email
                    },
                )
            }

            if (showUserNameDialog) {
                InputDialog(
                    title = "UserName",
                    inputLabel = "UserName",
                    inputValue = inputUserName,
                    onInputValueChange = { text ->
                        inputUserName = text
                    },
                    onConfirm = {
                        runCatching {
                            if (username.contains(" ").not()) {
                                updateConfig(context, inputUserName, email)
                                username = inputUserName
                            } else {
                                inputUserName = username
                                rkUtils.toast("Invalid Username")
                            }

                        }.onFailure { rkUtils.toast(it.message) }

                        showUserNameDialog = false
                    },
                    onDismiss = {
                        showUserNameDialog = false
                        inputUserName = username
                    },
                )
            }

            if (showTokenDialog) {
                activity?.window?.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
                )


                InputDialog(
                    title = "Github Token",
                    inputLabel = "Github Token",
                    inputValue = inputToken,
                    onInputValueChange = { text ->
                        inputToken = text
                    },
                    onConfirm = {
                        runCatching {
                            if (inputToken.contains("ghp")) {
                                updateToken(context, username, inputToken)
                                token = inputToken
                            } else {
                                "Invalid Github Token".toastIt()
                                inputToken = token
                            }

                        }.onFailure { it.message.toastIt() }
                        showTokenDialog = false
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    },
                    onDismiss = {
                        showTokenDialog = false
                        inputToken = token
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    },
                )

            }
        }
    }
}

suspend fun loadGitConfig(context: Context): Pair<String, String> {
    return withContext(Dispatchers.IO) {
        val config = context.filesDir.child(".gitconfig")
        if (config.exists()) {
            runCatching {
                val text = config.readText()
                val matchResult =
                    Regex("""\[user]\s*name\s*=\s*(\S+)\s*email\s*=\s*(\S+)""").find(text)
                val name = matchResult?.groupValues?.get(1) ?: "root"
                val email = matchResult?.groupValues?.get(2) ?: "example@mail.com"
                return@withContext Pair(name, email)
            }.getOrElse {
                Pair("root", "example@mail.com")
            }
        } else {
            Pair("root", "example@mail.com")
        }
    }
}

private fun updateConfig(context: Context, username: String, email: String) {
    val config = context.filesDir.child(".gitconfig").createFileIfNot()
    config.writeText(
        """[user]
 name = $username
 email = $email
[color]
 ui = true
 status = true
 branch = true
 diff = true
 interactive = true
[credential]
 helper = store
"""
    )
}

private fun updateToken(context: Context, username: String, token: String) {
    val cred = context.filesDir.child(".git-credentials").createFileIfNot()
    cred.writeText("https://$username:$token@github.com")
}

private inline fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

suspend fun getToken(context: Context): String {
    return withContext(Dispatchers.IO) {
        val cred = context.filesDir.child(".git-credentials")
        if (cred.exists()) {
            val regex = """https://([^:]+):([^@]+)@github.com""".toRegex()
            val matchResult = regex.find(cred.readText())
            return@withContext matchResult?.groupValues?.get(2) ?: ""
        }
        ""
    }
}
