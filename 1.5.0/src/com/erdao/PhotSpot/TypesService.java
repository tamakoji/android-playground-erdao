/*
 * Copyright (C) 2009 Huan Erdao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.erdao.PhotSpot;

/**
 * enum class for Content Provider
 * @author Huan Erdao
 */
public class TypesService {
	/** this is static class. cannot call constructor */
	private TypesService(){};
	/** Use Panoramio */
	public final static int PANORAMIO = 0;
	/** Use Picasa Web Albums */
	public final static int PICASAWEB = 1;
	/** Use Flickr */
	public final static int FLICKR = 2;

}
