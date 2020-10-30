package id.runup.realtimemaps

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_maps.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var mSocket: Socket
    private var mMarker: WeakHashMap<Int, Marker> = WeakHashMap<Int, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val opts = IO.Options()
        opts.path = "/socket.io"
        opts.transports = arrayOf("websocket")
        mSocket = IO.socket("http://ws.malangsatujiwa.com", opts)
        mSocket.connect()
            .on(Socket.EVENT_CONNECT) {
                Log.e("SOCKET", "Connected" + Gson().toJson(it))
            }
            .on(Socket.EVENT_CONNECT_ERROR) {
                Log.e("SOCKET", "CONERR Cant connected" + Gson().toJson(it))
            }
            .on(Socket.EVENT_ERROR) {
                Log.e("SOCKET", "ERR Cant connected" + Gson().toJson(it))
            }

        btnSimulate.setOnClickListener {
            val data = JSONObject()
            data.put("id", 8)
            data.put("latitude", -7.6092488)
            data.put("longitude", 112.2627059)
            Log.e("data", Gson().toJson(data))

            mSocket.emit("locations", data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket.disconnect();
        mSocket.off("locations");
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mSocket.on("locations", onNewMessage)
    }

    private val onNewMessage = Emitter.Listener { args ->
        Log.e("onNewMessage", "Emitted")
        runOnUiThread(Runnable {
            val data = args[0] as JSONObject

            try {
                val lat: Double = data.getString("latitude").toDouble()
                val lon: Double = data.getString("longitude").toDouble()
                val id: Int = data.getString("id").toInt()

                val newLatLng = LatLng(lat, lon)

                if(mMarker[id] == null) {
                    mMarker[id] = mMap.addMarker(MarkerOptions().position(newLatLng).title("Marker Covid"))
                } else {
                    mMarker[id]!!.position = newLatLng
                }
            } catch (e: JSONException) {
                return@Runnable
            }
        })
    }
}