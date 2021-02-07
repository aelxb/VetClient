package ru.konder.vetclient

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.net.Socket
import java.util.*

class MainActivity : AppCompatActivity() {
    private val address = "192.168.137.1"
    var active:Boolean = false
    private val port = 5000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        CoroutineScope(Dispatchers.IO).launch {
            clientGet(address, port)
        }
    }

    fun buttonRateClick(view: View) {
        CoroutineScope(Dispatchers.IO).launch {
            val button = findViewById<Button>(view.id)
            clientSend(address, port, button.text.toString())
        }
    }

    private suspend fun clientSend(address: String, port: Int, value: String) {
        while (true) {
            var connection = Socket(address, port)
            var writer = connection.getOutputStream()
            writer.write(value.toByteArray())
            var reader = Scanner(connection.getInputStream())
            if(reader.hasNextInt()){
                if (reader.nextInt() == 1)
                    break
            }
        }
        Toast.makeText(this, "Оценка успешно отправлена", Toast.LENGTH_LONG)
    }

    private suspend fun clientGet(address: String, port: Int) {
            var connection = Socket(address, port)
            var writer = connection.getOutputStream()
            var reader = DataInputStream(connection.getInputStream())
            var message = ByteArray(9)
            var sizePicture: Int
            while (String(message) != "Connected") {
                writer.write("Connect".toByteArray())
                reader.read(message, 0, message.size)
                if (String(message) == "Connected") {
                    /*runOnUiThread {
                        var text = findViewById<TextView>(R.id.connectionInfo)
                        text.text = String(message)
                    }*/
                    writer.close()
                    reader.close()
                    connection.close()
                    active = true
                }
            }

            connection = Socket(address, port)
            writer = connection.getOutputStream()
            reader = DataInputStream(connection.getInputStream())

            writer.write("GO".toByteArray())

            var bytesRead: Int
            var picSize = ByteArray(10)
            bytesRead = reader.read(picSize, 0, picSize.size)

            sizePicture = String(picSize).toInt()
            Log.e("SIZE", sizePicture.toString())

            writer.close()
            reader.close()
            connection.close()

            var pic = ByteArray(sizePicture)
            var picCounter: Int = 0;
            var good = true
            while (picCounter < pic.size) {
                connection = Socket(address, port)
                writer = connection.getOutputStream()
                reader = DataInputStream(connection.getInputStream())

                if (good) {
                    writer.write("IMAGE".toByteArray())
                } else {
                    writer.write("ERRORIMAGE".toByteArray())
                }


                var bytesCount: Int
                var partSize = ByteArray(1024)
                bytesCount = reader.read(partSize, 0, partSize.size)

                if (bytesCount == 1024 || bytesCount == (pic.size % 1024)) {
                    for (i: Int in 0..bytesCount - 1) {
                        if (picCounter < pic.size) {
                            pic[picCounter] = partSize[i]
                            picCounter++
                        } else {
                            break
                        }
                    }

                    good = true
                } else {
                    good = false
                }

                writer.close()
                reader.close()
                connection.close()
            }

            runOnUiThread {
                var image = findViewById<ImageView>(R.id.imageEmployee)
                image.setImageBitmap(getImage.StringToBitMap(pic, picCounter))
            }
        }
    }