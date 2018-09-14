package de.nuttercode.androidprojectss2018.test;

import de.nuttercode.androidprojectss2018.csi.ClientConfiguration;
import de.nuttercode.androidprojectss2018.csi.TagStore;
import de.nuttercode.androidprojectss2018.example.StoreListenerExample;

/**
 * Test LBRClient connection
 * 
 * @author Johannes B. Latzel
 *
 */
public class TagQueryTestLocal {

	public static void main(String[] args) {

		ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setServerDNSName("localhost");
		TagStore tagStore = new TagStore(clientConfiguration);
		tagStore.addStoreListener(new StoreListenerExample<>());
		System.out.println(tagStore.refresh());

	}

}