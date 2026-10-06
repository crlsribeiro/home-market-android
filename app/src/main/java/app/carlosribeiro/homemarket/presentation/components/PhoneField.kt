@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.PhoneCountry
import java.util.Locale

/** iOS phone field: country picker with the dial code, then the number with the country's mask. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneField(
    phone: String,
    country: PhoneCountry,
    onPhoneChange: (String) -> Unit,
    onCountryChange: (PhoneCountry) -> Unit,
    label: String = stringResource(R.string.account_phone)
) {
    var expanded by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = "${country.flag} ${country.dialCode}",
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text(stringResource(R.string.account_country)) },
                modifier = Modifier
                    .width(132.dp)
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                PhoneCountry.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("${option.flag} ${option.displayName()} (${option.dialCode})") },
                        onClick = {
                            expanded = false
                            onCountryChange(option)
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text(label) },
            placeholder = { Text(country.placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f)
        )
    }
}

/** The country name in the device language. */
private fun PhoneCountry.displayName(): String = Locale.Builder().setRegion(isoCode).build()
    .getDisplayCountry(Locale.getDefault())
