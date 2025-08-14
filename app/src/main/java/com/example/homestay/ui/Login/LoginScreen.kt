package com.example.HomeStay.ui.Login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homestay.R

@Composable
fun LoginScreen(modifier: Modifier = Modifier){
    Column(modifier = Modifier
        .background(color = Color(0xFFF8F8F6))
        .fillMaxWidth()) {

        Text(
            text = stringResource(R.string.login),
            fontSize = 50.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .padding(top = 120.dp)
                .padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.padding(vertical = 10.dp))

        Text(
            text = stringResource(R.string.username),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 60.dp)
                .padding(horizontal = 20.dp)
        )

        SubmitButton()
    }
}

@Composable
fun SubmitButton(modifier: Modifier = Modifier){
    OutlinedButton(onClick = {}) {
        Text(
            text = stringResource(R.string.submit),
            /*fontSize = 22.sp,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color()*/
            )
    }
}