package com.grupo2.ashley.login

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.grupo2.ashley.ui.theme.ASHLEYTheme

class RecuperarUI : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ASHLEYTheme {
                RecuperarContra()
            }
        }
    }
}

@Composable
fun RecuperarContra(){
    var email by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
        
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "Escriba su correo",
            fontWeight = FontWeight.Bold
        )
        Text(
            "Le llegará un correo con las instrucciones",
            modifier = Modifier.alpha(0.5f)
        )
        Spacer(
            modifier = Modifier.height(6.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = {
                Text(
                    "Email",
                    modifier = Modifier.alpha(0.3f)
                )
            }
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        Button(
            onClick = {
                if(!email.isEmpty()){
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if(task.isSuccessful) {
                               Toast.makeText(context,"Correo Enviado Satisfactoriamente",Toast.LENGTH_SHORT).show()
                           } else {
                                Toast.makeText(context,"Correo no registrado",Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            },
            content = {
                Text(
                    "Enviar Correo",
                    fontSize = 16.sp,
                    color = Color.White
                )
            },
            modifier = Modifier.size(height = 52.dp, width = 286.dp)
        )
    }
}

