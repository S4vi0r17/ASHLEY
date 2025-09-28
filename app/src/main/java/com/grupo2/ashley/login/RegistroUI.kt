package com.grupo2.ashley.login

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grupo2.ashley.R
import com.grupo2.ashley.ui.theme.ASHLEYTheme

class RegistroUI : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ASHLEYTheme {
                val viewModel: RegistroViewModel = viewModel()
                Registro(
                    viewModel
                )
            }
        }
    }
}

@Composable
fun Registro(
    viewModel: RegistroViewModel
){
    val email = viewModel.email.collectAsState().value
    val password = viewModel.password.collectAsState().value
    val rPassword = viewModel.rPassword.collectAsState().value
    val visibility1 = viewModel.visibility1.collectAsState().value
    val visibility2 = viewModel.visibility2.collectAsState().value
    val image = R.drawable.uicon512
    val context = LocalContext.current

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
            alignment = Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize()
    ) {
        item{
            Image(
                painter = painterResource(image),
                modifier = Modifier.alpha(0.5f) ,
                contentDescription = null
            )
        }
        item {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    viewModel.onEmailChange(it)
                },
                label = {
                    Text(
                        "Email"
                    )
                }
            )
        }
        item {
            OutlinedTextField(
                value = password,
                visualTransformation = if (visibility1) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (visibility1) Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    IconButton(
                        onClick = {
                            viewModel.toggleVisibility1()
                        }
                    ) {
                        Icon(
                            imageVector = image,
                            contentDescription = null
                        )
                    }
                },
                onValueChange = {
                    viewModel.onPasswordChange(it)
                },
                label = {
                    Text(
                        "Contraseña"
                    )
                }
            )
        }
        item {
            OutlinedTextField(
                value = rPassword,
                visualTransformation = if (visibility2) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (visibility2) Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    IconButton(
                        onClick = {
                            viewModel.toggleVisibility2()
                        }
                    ) {
                        Icon(
                            imageVector = image,
                            contentDescription = null
                        )
                    }
                },
                onValueChange = {
                    viewModel.onRPasswordChange(it)
                },
                label = {
                    Text(
                        "Repetir Contraseña"
                    )
                }
            )
        }
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = {
                    if(!viewModel.validateEmail().first){
                        Toast.makeText(context,viewModel.validateEmail().second,Toast.LENGTH_SHORT).show()
                    } else if (!viewModel.validatePassword().first){
                        Toast.makeText(context,viewModel.validatePassword().second,Toast.LENGTH_SHORT).show()
                    } else {
                        val success = viewModel.registrarUsuario()
                        Toast.makeText(context,success,Toast.LENGTH_SHORT).show()
                    }
                },
                content = {
                    Text(
                        "Registrar",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                },
                modifier = Modifier.size(height = 52.dp, width = 286.dp)
            )
        }
    }
}