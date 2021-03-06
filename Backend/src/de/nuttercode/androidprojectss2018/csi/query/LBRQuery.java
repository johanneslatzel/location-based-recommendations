package de.nuttercode.androidprojectss2018.csi.query;

import de.nuttercode.androidprojectss2018.csi.config.ClientConfiguration;
import de.nuttercode.androidprojectss2018.csi.config.TagPreferenceConfiguration;
import de.nuttercode.androidprojectss2018.csi.pojo.ScoredEvent;

/**
 * used to query {@link ScoredEvent}s from the {@link LBRServer}
 * 
 * @author Johannes B. Latzel
 *
 */
public class LBRQuery extends Query<ScoredEvent> {

	private static final long serialVersionUID = 2485145114980361102L;
	private double radius;
	private double longitude;
	private double latitude;
	private TagPreferenceConfiguration tagPreferenceConfiguration;

	/**
	 * 
	 * @param clientConfiguration
	 * @throws IllegalArgumentException
	 *             if {@link Query#Query(ClientConfiguration)} does
	 */
	public LBRQuery(ClientConfiguration clientConfiguration) {
		super(clientConfiguration);
		setClientConfiguration(clientConfiguration);
		longitude = 0.0;
		latitude = 0.0;
	}

	public TagPreferenceConfiguration getTagPreferenceConfiguration() {
		return tagPreferenceConfiguration;
	}

	public double getRadius() {
		return radius;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	@Override
	public void setClientConfiguration(ClientConfiguration clientConfiguration) {
		super.setClientConfiguration(clientConfiguration);
		radius = clientConfiguration.getRadius();
		tagPreferenceConfiguration = clientConfiguration.getTagPreferenceConfiguration();
	}

}
