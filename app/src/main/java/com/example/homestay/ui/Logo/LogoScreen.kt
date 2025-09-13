package com.example.homestay.ui.Logo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homestay.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun LogoScreen(isTablet: Boolean,
               onLoginButtonClicked: () -> Unit = {},
               onSignUpButtonClicked: () -> Unit = {},
               modifier: Modifier = Modifier){

    val scrollState = rememberScrollState()
    val topSpacing = if (isTablet) 10.dp else 180.dp
    val logoSize = if (isTablet) 300.dp else 280.dp
    val titleFontSize = if (isTablet) 36.sp else 30.sp
    val buttonSpacing = if (isTablet) 28.dp else 55.dp
    val betweenButtonsSpacing = if (isTablet) 28.dp else 35.dp

    if (isTablet) {
        //Tablet layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(color = Color(0xFFFEF9F3))
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo",
                modifier = modifier
                    .padding(top = topSpacing)
                    .size(logoSize)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Welcome to SKYBNB!",
                modifier = modifier.padding(horizontal = 20.dp),
                fontSize = titleFontSize,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(buttonSpacing))

            //Login Button
            ElevatedButton(onClick = onLoginButtonClicked,
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF446F5C),
                    contentColor = Color.White)){
                Text(
                    text = stringResource(R.string.login),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(betweenButtonsSpacing))

            //Sign Up Button
            ElevatedButton(onClick = onSignUpButtonClicked,
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF446F5C),
                    contentColor = Color.White)){
                Text(
                    text = stringResource(R.string.sign_up),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    } else {
        //Phone layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xFFFEF9F3))
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo",
                modifier = modifier
                    .padding(top = topSpacing)
                    .size(logoSize)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(60.dp))

            //Login Button
            ElevatedButton(onClick = onLoginButtonClicked,
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF446F5C),
                    contentColor = Color.White)){
                Text(
                    text = stringResource(R.string.login),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(betweenButtonsSpacing))

            //Sign Up Button
            ElevatedButton(onClick = onSignUpButtonClicked,
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF446F5C),
                    contentColor = Color.White)){
                Text(
                    text = stringResource(R.string.sign_up),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}