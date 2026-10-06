@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.presentation.theme.ControlHeight
import app.carlosribeiro.homemarket.presentation.theme.brandColors

/**
 * iOS `BrandField`: the uppercase label above a white, thin-bordered field with a leading icon, at the
 * shared 54 dp height. Without [showLabel] the caller shows the label itself (it is still read out).
 * Password fields are masked and get a show/hide toggle. A non-null [error] marks the field as invalid
 * and replaces the supporting text.
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    placeholder: String? = null,
    required: Boolean = false,
    showLabel: Boolean = true,
    supportingText: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = formKeyboard(),
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val isPassword = keyboardOptions.keyboardType == KeyboardType.Password
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val helper = error ?: supportingText
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showLabel) BrandFieldLabel(text = label, required = required)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder ?: label) },
            leadingIcon = leadingIcon?.let { icon -> { Icon(painterResource(icon), contentDescription = null) } },
            trailingIcon = if (isPassword) {
                { PasswordToggle(visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible }) }
            } else {
                null
            },
            supportingText = helper?.let { text -> { Text(text) } },
            isError = error != null,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 2,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            colors = brandFieldColors(),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ControlHeight)
                .semantics { contentDescription = label }
        )
    }
}

/** Field colors of the iOS form: white container, gray border until focused, gray icons. */
@Composable
fun brandFieldColors(): TextFieldColors {
    val card = brandColors.card
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = card,
        unfocusedContainerColor = card,
        disabledContainerColor = card,
        errorContainerColor = card,
        unfocusedBorderColor = brandColors.cardBorder,
        disabledBorderColor = brandColors.cardBorder,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
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
