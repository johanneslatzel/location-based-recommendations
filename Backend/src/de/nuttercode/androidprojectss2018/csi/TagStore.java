package de.nuttercode.androidprojectss2018.csi;

import de.nuttercode.androidprojectss2018.csi.query.TagQuery;
import de.nuttercode.androidprojectss2018.lbrserver.LBRServer;

/**
 * stores all locally available {@link Tag}s and retreives new {@link Tag}s from
 * the {@link LBRServer}
 * 
 * @author Johannes B. Latzel
 *
 */
public class TagStore extends Store<Tag> {

	/**
	 * @param clientConfiguration
	 * @throws IllegalArgumentException
	 *             if {@link Store#Store(Query)} does
	 */
	public TagStore(ClientConfiguration clientConfiguration) {
		super(new TagQuery(clientConfiguration));
	}

}
