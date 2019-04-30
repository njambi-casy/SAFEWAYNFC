package com.example.safewaynfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static android.nfc.NdefRecord.*;


public class MainActivity extends AppCompatActivity {
    private TextView text;
    private PendingIntent pendingIntent;
    private  NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = findViewById(R.id.text);
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, "No NFC", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }
    @Override
    protected void onResume() {
        super.onResume();

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled())
                showWirelessSettings();

            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }
    protected void onPause(){
        super.onPause();
    }



    private void showWirelessSettings() {
        Toast.makeText(this, "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;

            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];

                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }

            }else {
                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                byte[] payload =  dumpTagData(tag).getByte();
                NdefRecord record = new NdefRecord(TNF_UNKNOWN, empty, id, payload);
                NdefMessage msg = new NdefMessage(new NdefRecord[] {record});
                msgs = new NdefMessage[] {msg};
            }

            displayMsgs(msgs);
        }
    }

      private void displayMsgs(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0)
            return;

        StringBuilder builder = new StringBuilder();
        List<ParsedNdefRecord> records = NdefMessageParser.parse(msgs[0]);
        final int size = records.size();

        for (int i = 0; i < size; i++) {
            ParsedNdefRecord record = records.get(i);
            String str = record.str();
            builder.append(str).append("\n");
        }

        text.setText(builder.toString());
    }
    public class DumpTagData extends {
        private byte dumpTagData(Tag tag) {
            StringBuilder sb = new StringBuilder();
            byte[] id = tag.getId();
            sb.append("ID (hex): ").append(toHex(id)).append('\n');
            sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n');
            sb.append("ID (dec): ").append(toDec(id)).append('\n');
            sb.append("ID (reversed dec): ").append(toReversedDec(id)).append('\n');

            String prefix = "android.nfc.tech.";
            sb.append("Technologies: ");
            for (String tech : tag.getTechList()) {
                sb.append(tech.substring(prefix.length()));
                sb.append(", ");
            }

            sb.delete(sb.length() - 2, sb.length());

            for (String tech : tag.getTechList()) {
                if (tech.equals(MifareClassic.class.getName())) {
                    sb.append('\n');
                    String type = "Unknown";

                    try {
                        MifareClassic mifareTag = MifareClassic.get(tag);

                        switch (mifareTag.getType()) {
                            case MifareClassic.TYPE_CLASSIC:
                                type = "Classic";
                                break;
                            case MifareClassic.TYPE_PLUS:
                                type = "Plus";
                                break;
                            case MifareClassic.TYPE_PRO:
                                type = "Pro";
                                break;
                        }
                        sb.append("Mifare Classic type: ");
                        sb.append(type);
                        sb.append('\n');

                        sb.append("Mifare size: ");
                        sb.append(mifareTag.getSize() + " bytes");
                        sb.append('\n');

                        sb.append("Mifare sectors: ");
                        sb.append(mifareTag.getSectorCount());
                        sb.append('\n');

                        sb.append("Mifare blocks: ");
                        sb.append(mifareTag.getBlockCount());
                    } catch (Exception e) {
                        sb.append("Mifare classic error: " + e.getMessage());
                    }
                }

                if (tech.equals(MifareUltralight.class.getName())) {
                    sb.append('\n');
                    MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                    String type = "Unknown";
                    switch (mifareUlTag.getType()) {
                        case MifareUltralight.TYPE_ULTRALIGHT:
                            type = "Ultralight";
                            break;
                        case MifareUltralight.TYPE_ULTRALIGHT_C:
                            type = "Ultralight C";
                            break;
                    }
                    sb.append("Mifare Ultralight type: ");
                    sb.append(type);
                }
            }

            return Byte.parseByte(sb.toString());
        }
        }
        private String toHex(byte[]bytes){
            StringBuilder sb = new StringBuilder();
            for (int i= bytes.length - 1; i>=0; --i) {
                int b = bytes[i] & 0xff;
                if (b < 0x10)
                    sb.append("0");
                sb.append(Integer.toHexString(b));
                if (i > 0) {
                    sb.append(" ");
                }
            }
            return sb.toString();

        }

        private String toReversedHex(byte[]bytes){
            StringBuilder sb = new StringBuilder();
            for (int i= 0; i < bytes.length; --i) {
                if (i > 0) {
                    sb.append(" ");
                }
                int b = bytes[i] & 0xff;
                sb.append(Integer.toHexString(b));
            }

            return sb.toString();

        }

        private long toDec(byte[] bytes){
            long result=0;
            long factor=1;
            for (int i = 0; i < bytes.length; ++i) {
                long value = bytes[i] & 0xffl;
                result += value * factor;
                factor *= 256l;
            }
            return result;
        }

        private long toReversedDec (byte[] bytes){
            long result = 0;
            long factor = 1;
            for(int i = bytes.length -1; i >= 0; --i){
                long value = bytes[i] & 0xffl;
                result += value * factor;
                factor *= 256l;
            }

            return result;
        }

    }


}