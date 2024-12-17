package com.example.nfcqsl

import android.annotation.SuppressLint
import android.content.Context
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareUltralight
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.TimeZone

class NFCReadActivity : AppCompatActivity() {

    private var tag: Tag? = null
    private var maxLength: Int = 0

    private fun isNumeric(s: String): Boolean {
        return try {
            s.toFloat()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun intToBytes2(n: Int): ByteArray {
        val b = ByteArray(4)
        for (i in 0..3) {
            b[i] = (n shr (24 - i * 8)).toByte()
        }
        return b
    }

    @SuppressLint("SimpleDateFormat", "WeekBasedYear", "NewApi", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nfcread)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val button: Button = findViewById<Button>(R.id.writeButton2)
        //val unlockButton: Button = findViewById<Button>(R.id.buttonUnlock)

        val myCallText: EditText = findViewById<EditText>(R.id.editTextMyCall)
        val toCallText: EditText = findViewById<EditText>(R.id.editTextToCall)
        val dateText: EditText = findViewById<EditText>(R.id.editTextDate)
        val timeText: EditText = findViewById<EditText>(R.id.editTextTime)
        val freqText: EditText = findViewById<EditText>(R.id.editTextFreq)
        val rstText: EditText = findViewById<EditText>(R.id.editTextRst)
        val noteText: EditText = findViewById<EditText>(R.id.editTextNote)

        val swlRadio: RadioButton = findViewById<RadioButton>(R.id.radioButtonSWL)
        val tnxRadio: RadioButton = findViewById<RadioButton>(R.id.radioButtonTNX)

        val modeSpinner: Spinner = findViewById<Spinner>(R.id.spinnerMode);

        val noteCapTextView: TextView = findViewById<TextView>(R.id.textViewNoteCap)

        val starArray = arrayOf("AM", "CW", "Eyeball", "FM", "FT4", "FT8", "RTTY", "SSB", "Others")
        val starAdapter: ArrayAdapter<String> = ArrayAdapter<String>(this,
            android.R.layout.simple_list_item_1, starArray)
        modeSpinner.adapter = starAdapter;
        val a = starArray.indexOf("CW")
        modeSpinner.setSelection(a)

        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val timeFormat = SimpleDateFormat("HHmm")
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        timeFormat.timeZone = TimeZone.getTimeZone("UTC")
        val timeNow = Date.from(Instant.now())
        val dateStr = dateFormat.format(timeNow)
        val timeStr = timeFormat.format(timeNow)

        dateText.setText(dateStr)
        timeText.setText(timeStr)

        /*
        unlockButton.setOnClickListener{
            val ul : MifareUltralight = MifareUltralight.get(tag)
            try {
                ul.connect()
                // Get into PWD_AUTH authentication
                ul.transceive(byteArrayOf(0x1B.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
                ul.writePage(43, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
                ul.writePage(44, byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
                ul.writePage(41, byteArrayOf(0x04.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte()))
                Toast.makeText(this, "Unlock NTag successfully!", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: TagLostException) {
                Toast.makeText(this, "Tag Connection Lost.", Toast.LENGTH_SHORT).show()
                this.finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Password Error or Connection Lost.", Toast.LENGTH_SHORT).show()
                this.finish()
            } finally {
                try {
                    ul.close()
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
        }

         */
        noteText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                var currentStr: String = s!!.toString()
                if(currentStr.toByteArray().size > maxLength) {
                    noteText.removeTextChangedListener(this)
                    while (currentStr.toByteArray().size > maxLength) {
                        currentStr = currentStr.substring(0, currentStr.length - 1)
                    }
                    noteText.setText(currentStr);
                    noteText.setSelection(currentStr.length)
                    noteText.addTextChangedListener(this)
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        button.setOnClickListener {
            val ul : MifareUltralight = MifareUltralight.get(tag)

            if(myCallText.text.isEmpty()) {
                Toast.makeText(this, "Your call sign could not left blank.", Toast.LENGTH_SHORT).show()
            } else if(toCallText.text.isEmpty()) {
                Toast.makeText(this, "To radio call sign could not left blank.", Toast.LENGTH_SHORT).show()
            } else if(dateText.text.isEmpty()) {
                Toast.makeText(this, "QSO Date could not left blank.", Toast.LENGTH_SHORT).show()
            } else if(timeText.text.isEmpty()) {
                Toast.makeText(this, "QSO time could not left blank.", Toast.LENGTH_SHORT).show()
            } else if(freqText.text.isNotEmpty() &&!isNumeric(freqText.text.toString())){
                Toast.makeText(this, "QSO Frequency format is invalid.", Toast.LENGTH_SHORT).show()
            } else {

                try {
                    ul.connect()
                    val isSWL: Byte = if (swlRadio.isChecked) 1 else 0
                    val isTNX: Byte = if (tnxRadio.isChecked) 1 else 0

                    ul.writePage(4, byteArrayOf(0x72, isSWL, isTNX, 0x00))

                    var callSign: ByteArray = myCallText.text.toString().toByteArray()
                    while (callSign.size <= 12) {
                        callSign += 0
                    }
                    ul.writePage(5, callSign.copyOfRange(0, 4))
                    ul.writePage(6, callSign.copyOfRange(4, 8))
                    ul.writePage(7, callSign.copyOfRange(8, 12))

                    var toCallSign: ByteArray = toCallText.text.toString().toByteArray()
                    while (toCallSign.size <= 16) {
                        toCallSign += 0
                    }
                    ul.writePage(8, toCallSign.copyOfRange(0, 4))
                    ul.writePage(9, toCallSign.copyOfRange(4, 8))
                    ul.writePage(10, toCallSign.copyOfRange(8, 12))
                    ul.writePage(11, toCallSign.copyOfRange(12, 16))

                    val timeStr1 = dateText.text.toString() + timeText.text.toString()
                    val dateFormat1 = SimpleDateFormat("yyyyMMddHHmm")
                    dateFormat1.timeZone = TimeZone.getTimeZone("UTC")
                    var timestamp1:Long = dateFormat1.parse(timeStr1)!!.time
                    val timestampByteArrayL: ByteArray = ByteArray(4)
                    val timestampByteArrayH: ByteArray = ByteArray(4)

                    timestamp1 = timestamp1 / 1000
                    for(i in 0..3) {
                        val timestampParse = timestamp1.rem(256)
                        timestamp1 /= 256
                        timestampByteArrayL[i] = timestampParse.toByte()
                    }
                    for(i in 0..3) {
                        val timestampParse = timestamp1.rem(256)
                        timestamp1 /= 256
                        timestampByteArrayH[i] = timestampParse.toByte()
                    }

                    ul.writePage(12, timestampByteArrayL)
                    ul.writePage(13, timestampByteArrayH)

                    if(freqText.text.isNotEmpty()){
                        var freqArray: ByteArray = freqText.text.toString().toByteArray()
                        while (freqArray.size <= 8) {
                            freqArray += 0
                        }
                        ul.writePage(14, freqArray.copyOfRange(0, 4))
                        ul.writePage(15, freqArray.copyOfRange(4, 8))
                    }
                    else{
                        ul.writePage(14, byteArrayOf(0,0,0,0))
                        ul.writePage(15, byteArrayOf(0,0,0,0))
                    }

                    var modeArray: ByteArray = modeSpinner.selectedItem.toString().toByteArray()
                    while (modeArray.size <= 8) {
                        modeArray += 0
                    }
                    ul.writePage(16, modeArray.copyOfRange(0, 4))
                    ul.writePage(17, modeArray.copyOfRange(4, 8))

                    if(rstText.text.isNotEmpty()){
                        var rstArray: ByteArray = rstText.text.toString().toByteArray()
                        while (rstArray.size <= 8) {
                            rstArray += 0
                        }
                        ul.writePage(18, rstArray.copyOfRange(0, 4))
                        ul.writePage(19, rstArray.copyOfRange(4, 8))
                    }
                    else{
                        ul.writePage(18, byteArrayOf(0,0,0,0))
                        ul.writePage(19, byteArrayOf(0,0,0,0))
                    }

                    val data: ByteArray = ul.readPages(0);
                    val maxLength = data[14].toInt()*8;

                    if(noteText.text.isNotEmpty()){
                        var noteArray: ByteArray = noteText.text.toString().toByteArray()
                        if (noteArray.size > 80) noteArray=noteArray.copyOfRange(0,80)
                        while (noteArray.size < 80) {
                            noteArray += 0
                        }
                        for(i in 20..< (maxLength - 64) / 4 + 20) {
                            ul.writePage(i, noteArray.copyOfRange((i-20)*4, (i-20)*4+4))
                        }
                    }
                    else{
                        for(i in 20..< (maxLength - 64) / 4 + 20) {
                            ul.writePage(i, byteArrayOf(0, 0, 0, 0))
                        }
                    }

                    val sharedPref = this.getSharedPreferences("mycall", MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putString("name", myCallText.text.toString())
                        apply()
                    }

                    Toast.makeText(this, "Write QSL info successfully! Please re-scan the Tag", Toast.LENGTH_SHORT)
                        .show()

                } catch (e: TagLostException) {
                    Toast.makeText(this, "Tag Connection Lost.", Toast.LENGTH_SHORT).show()
                    //this.finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Write QSL info error!", Toast.LENGTH_SHORT).show()
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

        tag = intent.getParcelableExtra<Tag>("tag")
        if(tag != null) {
            //Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Tag info error!", Toast.LENGTH_SHORT).show()
        }

        val tagCapacity = intent.getIntExtra("maxLength", 0);
        noteText.filters = arrayOf(InputFilter.LengthFilter(tagCapacity - 64))
        noteCapTextView.text = "Note (Maximum length: " + (tagCapacity - 64) + ")"
        maxLength = tagCapacity - 64

        val sharedPref = this.getSharedPreferences("mycall", MODE_PRIVATE)
        val myCallFromPref: String? = sharedPref.getString("name", "")
        if (myCallFromPref != null){
            myCallText.setText(myCallFromPref)
        }


    }
}