package no.nordicsemi.android.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.callback.FailCallback;
import no.nordicsemi.android.ble.callback.ReadProgressCallback;
import no.nordicsemi.android.ble.callback.SuccessCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.data.DataMerger;
import no.nordicsemi.android.ble.data.DataStream;

@SuppressWarnings("unused")
public final class ReadRequest extends Request {
	private ReadProgressCallback progressCallback;
	private DataReceivedCallback valueCallback;
	private DataMerger dataMerger;
	private DataStream buffer;
	private int count = 0;

	ReadRequest(final @NonNull Type type) {
		super(type);
	}

	ReadRequest(final @NonNull Type type, final @Nullable BluetoothGattCharacteristic characteristic) {
		super(type, characteristic);
	}

	ReadRequest(final @NonNull Type type, final @Nullable BluetoothGattDescriptor descriptor) {
		super(type, descriptor);
	}

	@Override
	@NonNull
	public ReadRequest done(final @NonNull SuccessCallback callback) {
		this.successCallback = callback;
		return this;
	}

	@Override
	@NonNull
	public ReadRequest fail(final @NonNull FailCallback callback) {
		this.failCallback = callback;
		return this;
	}

	@NonNull
	public ReadRequest with(final @NonNull DataReceivedCallback callback) {
		this.valueCallback = callback;
		return this;
	}

	/**
	 * Adds a merger that will be used to merge multiple packets into a single Data.
	 * The merger may modify each packet if necessary.
	 *
	 * @return the request
	 */
	@NonNull
	public ReadRequest merge(final @NonNull DataMerger merger) {
		this.dataMerger = merger;
		this.progressCallback = null;
		return this;
	}

	/**
	 * Adds a merger that will be used to merge multiple packets into a single Data.
	 * The merger may modify each packet if necessary.
	 *
	 * @return the request
	 */
	@NonNull
	public ReadRequest merge(final @NonNull DataMerger merger, final @NonNull ReadProgressCallback callback) {
		this.dataMerger = merger;
		this.progressCallback = callback;
		return this;
	}

	void notifyValueChanged(final @NonNull BluetoothDevice device, final byte[] value) {
		// With no value callback there is no need for any merging
		if (valueCallback == null)
			return;

		if (dataMerger == null) {
			valueCallback.onDataReceived(device, new Data(value));
		} else {
			if (progressCallback != null)
				progressCallback.onPacketReceived(device, value, count);
			if (buffer == null)
				buffer = new DataStream();
			if (dataMerger.merge(buffer, value, count++)) {
				valueCallback.onDataReceived(device, buffer.toData());
				buffer = null;
				count = 0;
			} // else
			// wait for more packets to be merged
		}
	}

	boolean hasMore() {
		return count > 0;
	}
}
