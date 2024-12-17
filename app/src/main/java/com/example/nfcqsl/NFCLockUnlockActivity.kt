package com.example.nfcqsl

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareUltralight
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.TimeZone

class NFCLockUnlockActivity : AppCompatActivity() {

    private var encrypted = false
    private var tag : Tag? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nfclock_unlock)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val button: Button = findViewById<Button>(R.id.buttonLU)
        val textViewCFM: TextView = findViewById<Button>(R.id.textViewCFM)
        val editTextPassword: EditText = findViewById<EditText>(R.id.editTextPassword)
        val editTextCFM: EditText = findViewById<EditText>(R.id.editTextCFM)
        val checkBoxShowPassword: CheckBox = findViewById<CheckBox>(R.id.checkBoxShowPassword)

        tag = intent.getParcelableExtra<Tag>("tag")
        if(tag != null) {
            //Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Tag info error!", Toast.LENGTH_SHORT).show()
        }

        encrypted = intent.getBooleanExtra("encrypted", false);
        if (encrypted){
            button.text = "Unlock this NFC QSL"
            textViewCFM.visibility = TextView.INVISIBLE
            editTextCFM.visibility = EditText.INVISIBLE
        } else {
            button.text = "Lock this NFC QSL"
            textViewCFM.visibility = TextView.VISIBLE
            editTextCFM.visibility = EditText.VISIBLE
        }

        checkBoxShowPassword.setOnCheckedChangeListener {
            compoundButton, b ->
            if(b){
                editTextPassword.inputType = InputType.TYPE_CLASS_TEXT
                editTextCFM.inputType = InputType.TYPE_CLASS_TEXT
            } else {
                editTextPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                editTextCFM.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        }

        button.setOnClickListener {
            val ul : MifareUltralight = MifareUltralight.get(tag)

            if (editTextPassword.text.length < 8) {
                Toast.makeText(this, "Password must have length of 8.", Toast.LENGTH_SHORT).show()
            } else if ((!encrypted) and (editTextPassword.text.toString() != editTextCFM.text.toString())) {
                Toast.makeText(this, "Password in two input box must be identical.", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    ul.connect()
                    var passwordInt = editTextPassword.text.toString().toLong(16)
                    val passwordByteArray = ByteArray(4)
                    for (i in 0..3){
                        passwordByteArray[i] = (passwordInt % 256).toByte()
                        passwordInt /= 256
                    }
                    if (encrypted)
                    {
                        ul.transceive(byteArrayOf(0x1B.toByte()) + passwordByteArray)
                        ul.writePage(43, passwordByteArray)
                        ul.writePage(44, byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
                        //disable write protection
                        ul.writePage(41, byteArrayOf(0x04.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte()))
                        Toast.makeText(this, "Unlock NTag successfully! Please re-scan the Tag.", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        ul.writePage(43, passwordByteArray)
                        ul.writePage(44, byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
                        // enable write protection
                        ul.writePage(41, byteArrayOf(0x04.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
                        Toast.makeText(this, "Lock NTag successfully! Please re-scan the Tag.", Toast.LENGTH_SHORT)
                            .show()
                    }
                } catch (e: TagLostException) {
                    Toast.makeText(this, "Tag Connection Lost.", Toast.LENGTH_SHORT).show()
                    //this.finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Password Error or Connection Lost.", Toast.LENGTH_SHORT).show()
                    //this.finish()
                } finally {
                    try {
                        ul.close()
                        this.finish()
                    } catch (e: IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}