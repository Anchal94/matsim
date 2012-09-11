/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalTravelTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.mobsim.qsim.multimodalsimengine.router.util;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.vehicles.Vehicle;

public interface MultiModalTravelTime extends PersonalizableTravelTime {

	public double getModalLinkTravelTime(Link link, double time, String transportMode, Person person, Vehicle vehicle);
	
	/**
	 * Define the transport mode that should be used if getLinkTravelTime(...) is called.
	 * Useful for backwards compatibility with code that is not designed to be multi-modal,
	 * e.g. a LeastCostPathCalculator. Before calculating the path, the transport mode
	 * can be set. As a result, the calculator will use the correct travel times for
	 * the given mode.
	 */
	public void setTransportMode(String transportMode);
}
