@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import app.carlosribeiro.homemarket.R

/**
 * Single-line Material 3 form field with an optional leading icon and supporting text. Password fields
 * are masked and get a show/hide toggle. A non-null [error] marks the field as invalid and replaces the
 * supporting text.
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    supportingText: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = formKeyboard(),
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val isPassword = keyboardOptions.keyboardType == KeyboardType.Password
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val helper = error ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { icon -> { Icon(painterResource(icon), contentDescription = null) } },
        trailingIcon = if (isPassword) {
            { PasswordToggle(visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible }) }
        } else {
            null
        },
        supportingText = helper?.let { text -> { Text(text) } },
        isError = error != null,
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordToggle(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
            contentDescription = stringResource(if (visible) R.string.password_hide else R.string.password_show)
        )
    }
}

/** Keyboard options for form fields: no autocorrect, "next" by default. */
fun formKeyboard(
    type: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
): KeyboardOptions = KeyboardOptions(
    keyboardType = type,
    imeAction = imeAction,
    capitalization = capitalization,
    autoCorrectEnabled = false
)
