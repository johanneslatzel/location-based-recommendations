package de.nuttercode.androidprojectss2018.lbrserver;

import de.nuttercode.androidprojectss2018.csi.Event;
import de.nuttercode.androidprojectss2018.csi.ScoredEvent;

/**
 * interface for different Event scoring mechanisms
 * 
 * @author Johannes B. Latzel
 *
 */
public interface EventScoreCalculator {

	/**
	 * processes the event and calculates its score - the score must satisfy the
	 * boundaries specified in {@link ScoredEvent}
	 * 
	 * @param event
	 * @return event + score
	 */
	ScoredEvent scoreEvent(Event event);

}