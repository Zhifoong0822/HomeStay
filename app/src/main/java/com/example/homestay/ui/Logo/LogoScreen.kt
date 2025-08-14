package com.example.HomeStay.ui.Logo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homestay.R


@Composable
fun LogoScreen(onLoginButtonClicked: () -> Unit = {},
               modifier: Modifier = Modifier){
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Color(0xFFF8F8F6))){

        LogoImage(modifier = modifier
            .align(Alignment.CenterHorizontally))

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = stringResource(R.string.choose_role),
            modifier = modifier.padding(horizontal = 20.dp),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(18.dp))

        RoleSelectionButton()

        Spacer(modifier = Modifier.height(55.dp))

        LoginButton(modifier = Modifier.align(Alignment.CenterHorizontally)
            .fillMaxWidth()
            .padding(horizontal = 64.dp),
            onLoginButtonClicked = onLoginButtonClicked)

        Spacer(modifier = Modifier.height(30.dp))

        SignUpButton(modifier = Modifier.align(Alignment.CenterHorizontally)
            .fillMaxWidth()
            .padding(horizontal = 64.dp))
    }
}

@Composable
fun LogoImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo),
        contentDescription = "Logo",
        modifier = modifier
            .padding(top = 155.dp)
            .size(290.dp)
    )
}

@Composable
fun RoleSelectionButton(modifier: Modifier = Modifier){
    Row(modifier = Modifier.padding(horizontal = 16.dp)){
        OutlinedButton(onClick = {},
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFF4C7F68),
                contentColor = Color.White)){
            Text(
                text = stringResource(R.string.host),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = modifier.width(8.dp))

        OutlinedButton(onClick = {},
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFF4C7F68),
                contentColor = Color.White)){
            Text(
                text = stringResource(R.string.guest),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun LoginButton(modifier: Modifier = Modifier,
                onLoginButtonClicked: () -> Unit = {}){
    OutlinedButton(onClick = onLoginButtonClicked,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF4C7F68),
            contentColor = Color.White)){
        Text(
            text = stringResource(R.string.login),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun SignUpButton(modifier: Modifier = Modifier){
    OutlinedButton(onClick = {},
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF4C7F68),
            contentColor = Color.White)){
        Text(
            text = stringResource(R.string.sign_up),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Preview
@Composable
fun LogoScreenPreview(){
    LogoScreen()
}