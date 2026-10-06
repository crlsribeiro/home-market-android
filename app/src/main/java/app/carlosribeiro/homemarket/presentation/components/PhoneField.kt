@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.PhoneCountry
import app.carlosribeiro.homemarket.presentation.theme.ControlHeight
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BrandFieldLabel(label)
        PhoneRow(phone, country, onPhoneChange, onCountryChange, label, expanded) { expanded = it }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneRow(
    phone: String,
    country: PhoneCountry,
    onPhoneChange: (String) -> Unit,
    onCountryChange: (PhoneCountry) -> Unit,
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val countryLabel = stringResource(R.string.account_country)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
            OutlinedTextField(
                value = "${country.flag} ${country.dialCode}",
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = MaterialTheme.shapes.medium,
                colors = brandFieldColors(),
                modifier = Modifier
                    .width(136.dp)
                    .heightIn(min = ControlHeight)
                    .semantics { contentDescription = countryLabel }
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                PhoneCountry.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("${option.flag} ${option.displayName()} (${option.dialCode})") },
                        onClick = {
                            onExpandedChange(false)
                            onCountryChange(option)
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            placeholder = { Text(country.placeholder) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_phone), contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = brandFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = ControlHeight)
                .semantics { contentDescription = label }
        )
    }
}

/** The country name in the device language. */
private fun PhoneCountry.displayName(): String = Locale.Builder().setRegion(isoCode).build()
    .getDisplayCountry(Locale.getDefault())
