package com.example.nfcqsl

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareUltralight
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class MainActivity : AppCompatActivity() {

    private var mNfcAdapter: NfcAdapter? = null
    private var pIntent: PendingIntent? = null
    private var tag: Tag? = null

    private var maxLength: Int = 0
    private var encrypted: Boolean = false

    //private val text: TextView = null

    private fun byteToInt2(b: ByteArray): Int {
        return (((b[0].toInt()) shl 24) + ((b[1].toInt()) shl 16)
                + ((b[2].toInt()) shl 8) + b[3])
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val button: Button = findViewById<Button>(R.id.writeButton)
        val buttonAbout: Button = findViewById<Button>(R.id.buttonAbout)
        val buttonLockUnlock: Button = findViewById<Button>(R.id.buttonLockUnlock)

        button.setOnClickListener {
            val intent = Intent(this, NFCReadActivity::class.java)
            intent.putExtra("tag", tag)
            intent.putExtra("maxLength", maxLength)
            startActivity(intent)
        }

        buttonLockUnlock.setOnClickListener {
            val intent = Intent(this, NFCLockUnlockActivity::class.java)
            intent.putExtra("tag", tag)
            intent.putExtra("encrypted", encrypted)
            startActivity(intent)
        }

        buttonAbout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("This APP is developed by BH9DWE.\n" +
                        "How to use:\n" +
                        "\tJust put NFC tag on the QSL card near the back of your phone, " +
                        "then QSL info in the NFC tag (or empty) will display. " +
                        "If the NFC is available, \"Write a QSL\" button will be visible. " +
                        "You can push the button to log your QSL info into the NFC tag.\n\n" +
                        "For further information, please visit our GitHub page.")
                .setPositiveButton("OK")
                {
                    dialog, which ->
                    dialog.dismiss()
                }
                .setNeutralButton("GitHub")
                {
                    dialog, which ->
                    val intent: Intent = Intent(Intent.ACTION_VIEW)
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setData(Uri.parse("https://github.com/Yurihou/NFC_QSL_Android/"))
                    startActivity(intent)
                }
                .create().show()
        }

        //text = findViewById<TextView>(R.id.textView)
        initNfc()
    }

    private fun initNfc() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pIntent = PendingIntent.getActivity(
            this, 0,  //在Manifest里或者这里设置当前activity启动模式，否则每次向阳NFC事件，activity会重复创建
            // 当然也要按照具体情况来，你设置成singleTask也不是不行，
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            0
        )
    }

    @SuppressLint("DefaultLocale", "NewApi")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        //这里必须setIntent，set  NFC事件响应后的intent才能拿到数据
        setIntent(intent)
        val button: Button = findViewById<Button>(R.id.writeButton)
        val text: TextView = findViewById<TextView>(R.id.textView)
        val buttonLockUnlock: Button = findViewById<Button>(R.id.buttonLockUnlock)
        tag = getIntent().getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if(tag != null) {
            // card info process
            val typeDesc: String
            if (!tag!!.techList.contains("android.nfc.tech.MifareUltralight")) {
                text.text = "Invalid NFC Tag."
            }
            else {
                val ul : MifareUltralight = MifareUltralight.get(tag)

                var info: String = ""
                try {
                    ul.connect();

                    val data: ByteArray = ul.readPages(0);
                    if (data[12].toUByte() == 0xE1u.toUByte() && data[13].toUByte() == 0x10u.toUByte() && data[15].toUByte() == 0x00u.toUByte()){
                        info = "Succeed reading NFC Tag!\n"
                        maxLength = data[14].toInt()*8;
                        val serialNum: String = String.format("%02X%02X%02X%02X%02X%02X%02X",
                            data[0], data[1], data[2], data[4], data[5], data[6], data[7])
                        info += "NFC Serial Number: "
                        info += serialNum
                        info += "\n"
                        
                        val data2: ByteArray = ul.readPages(4);
                        if (data2[0].toUByte() == 0x72u.toUByte()) {
                            info += "QSL Info:\n"

                            info += if (data2[1].toUByte() == 0x01u.toUByte()) "\tTo Confirm your report:\n"
                            else "\tTo Confirm our QSO:\n"

                            val pseTnxStr = if (data2[2].toUByte() == 0x01u.toUByte()) "TNX" else "PSE your QSL"

                            val myCallStr = String(data2.copyOfRange(4,16), Charsets.UTF_8)
                            info += "\tFrom: \t"
                            info += myCallStr
                            info += "\n"

                            val data3: ByteArray = ul.readPages(8)
                            val toCallStr = String(data3, Charsets.UTF_8)
                            info += "\tTo: \t"
                            info += toCallStr
                            info += "\n"

                            val data4: ByteArray = ul.readPages(12)
                            var timestampLong: Long = 0;
                            var timestampLongProducter: Long = 1;
                            for (i in 0..7) {
                                timestampLong += data4[i].toUByte().toLong()*timestampLongProducter;
                                timestampLongProducter *= 256
                            }

                            val dateFormat = SimpleDateFormat("yyyyMMdd")
                            val timeFormat = SimpleDateFormat("HHmm")
                            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                            timeFormat.timeZone = TimeZone.getTimeZone("UTC")
                            val timeNow = Date.from(Instant.ofEpochMilli(timestampLong*1000))
                            val dateStr = dateFormat.format(timeNow)
                            val timeStr = timeFormat.format(timeNow)

                            info += "\tQSO Date: \t"
                            info += dateStr
                            info += "\n"
                            info += "\tQSO Time UTC: \t"
                            info += timeStr
                            info += "\n"

                            val freqStr = String(data4.copyOfRange(8,16), Charsets.UTF_8)
                            info += "\tFreq: \t"
                            info += freqStr
                            info += " MHz\n"

                            val data5: ByteArray = ul.readPages(16)

                            val modeStr = String(data5.copyOfRange(0,8), Charsets.UTF_8)
                            info += "\tMode: \t"
                            info += modeStr
                            info += "\n"

                            val rstStr = String(data5.copyOfRange(8,16), Charsets.UTF_8)
                            info += "\tRST: \t"
                            info += rstStr
                            info += "\n"

                            info += "\tNote:\n\t"

                            var data6 = ByteArray(0)
                            for (i in 0..<(maxLength - 64)/4 step 4){
                                data6 += ul.readPages(20 + i)
                            }
                            //data6 += ul.readPages(24)
                            //data6 += ul.readPages(28)
                            //data6 += ul.readPages(32)
                            //data6 += ul.readPages(36)
                            data6 += 0

                            val noteStr = String(data6, Charsets.UTF_8)
                            info += noteStr
                            info += "\n"

                            info += pseTnxStr;
                            info += "\n"
                        }
                        else {
                            info += "Empty QSL card.\n"
                        }

                        val data7: ByteArray = ul.readPages((maxLength - 64) / 4 + 21)
                        if (data7[3] == 0xFF.toByte()){
                            info += "This QSL card is able to write in."
                            button.visibility = Button.VISIBLE
                            buttonLockUnlock.visibility = Button.VISIBLE
                            buttonLockUnlock.text = "Lock this QSL"
                            encrypted = false
                        } else {
                            info += "This QSL card is Locked."
                            button.visibility = Button.INVISIBLE
                            buttonLockUnlock.visibility = Button.VISIBLE
                            buttonLockUnlock.text = "Unlock this QSL"
                            encrypted = true
                        }
                    }
                    else {
                        info = "Invalid NFC Tag.";
                    }
                } catch (e : TagLostException) {
                    Toast.makeText(this, "Tag Connection Lost.", Toast.LENGTH_SHORT).show()
                } catch (e : Exception) {
                    // TODO: handle exception
                }finally{
                    try {
                        ul.close()
                        text.text = info
                    } catch (e : IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }


            }

        } else {
            text.text = "Failed to get NFC info."
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("FlashTestNFC", "onResume")
        if (mNfcAdapter != null) {
            //添加intent-filter
            val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            val tag = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            val filters = arrayOf(ndef, tag, tech)


            //添加 ACTION_TECH_DISCOVERED 情况下所能读取的NFC格式，这里列的比较全，实际我这里是没有用到的，因为测试的卡是NDEF的
            val techList = arrayOf(
                arrayOf(
                    "android.nfc.tech.Ndef",
                    "android.nfc.tech.NfcA",
                    "android.nfc.tech.NfcB",
                    "android.nfc.tech.NfcF",
                    "android.nfc.tech.NfcV",
                    "android.nfc.tech.NdefFormatable",
                    "android.nfc.tech.MifareClassic",
                    "android.nfc.tech.MifareUltralight",
                    "android.nfc.tech.NfcBarcode"
                )
            )
            mNfcAdapter!!.enableForegroundDispatch(this, pIntent, filters, techList)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i("FlashTestNFC", "onPause")
        if (mNfcAdapter != null) {
            mNfcAdapter!!.disableForegroundDispatch(this)
        }
    }

    /**
     * 2进制to 16进制
     * @param src
     * @return
     */
    private fun bytesToHex(src: ByteArray?): String? {
        val sb = StringBuffer()
        if (src == null || src.size <= 0) {
            return null
        }
        var sTemp: String
        for (i in src.indices) {
            sTemp = Integer.toHexString(0xFF and src[i].toInt())
            if (sTemp.length < 2) {
                sb.append(0)
            }
            sb.append(sTemp.uppercase(Locale.getDefault()))
        }
        return sb.toString()
    }
}