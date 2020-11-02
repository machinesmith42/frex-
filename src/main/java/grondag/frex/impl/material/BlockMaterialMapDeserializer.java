/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.frex.impl.material;

import java.io.InputStreamReader;
import java.util.IdentityHashMap;

import com.google.gson.JsonObject;
import grondag.frex.Frex;
import grondag.frex.api.material.MaterialMap;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

@Internal
public class BlockMaterialMapDeserializer {

	public static void deserialize(Block block, Identifier idForLog, InputStreamReader reader, IdentityHashMap<BlockState, MaterialMap> map) {
		try {
			final JsonObject json = JsonHelper.deserialize(reader);
			final String idString = idForLog.toString();

			final MaterialMap globalDefaultMap = MaterialMapImpl.DEFAULT_MAP;
			@Nullable RenderMaterial defaultMaterial = null;
			MaterialMap defaultMap = globalDefaultMap;

			if (json.has("defaultMaterial")) {
				defaultMaterial = MaterialLoaderImpl.loadMaterial(idString, json.get("defaultMaterial").getAsString(), defaultMaterial);
				defaultMap = new SingleMaterialMap(defaultMaterial);
			}

			if (json.has("defaultMap")) {
				defaultMap = MaterialMapDeserializer.loadMaterialMap(idString + "#default", json.getAsJsonObject("defaultMap"), defaultMap, defaultMaterial);
			}

			JsonObject variants = null;

			if (json.has("variants")) {
				variants = json.getAsJsonObject("variants");

				if(variants.isJsonNull()) {
					Frex.LOG.warn("Unable to load variant material maps for " + idString + " because the 'variants' block is empty. Using default map.");
					variants = null;
				}
			}

			for(final BlockState state : block.getStateManager().getStates()) {
				MaterialMap result = defaultMap;

				if (variants != null)  {
					final String stateId = BlockModels.propertyMapToString(state.getEntries());
					result = MaterialMapDeserializer.loadMaterialMap(idString + "#" + stateId, variants.getAsJsonObject(stateId), defaultMap, defaultMaterial);
				}

				if (result != globalDefaultMap) {
					map.put(state, result);
				}
			}
		} catch (final Exception e) {
			Frex.LOG.warn("Unable to load block material map for " + idForLog.toString() + " due to unhandled exception:", e);
		}
	}
}
