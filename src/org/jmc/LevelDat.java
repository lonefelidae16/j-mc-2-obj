/*******************************************************************************
 * Copyright (c) 2012
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package org.jmc;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.jmc.NBT.*;
import org.jmc.util.Log;

/**
 * Class used for loading the level.dat file from the world save.
 * This file contains useful information about the save like the
 * player position and state in the game, his inventory etc.
 * @author danijel
 *
 */
public class LevelDat {

	/**
	 * Path to the save.
	 */
	private Path levelDir;
	/**
	 * The root of the NBT structure for this file.
	 */
	private TAG_Compound root;

	/**
	 * Main constructor.
	 * @param levelDir path to the save
	 */
	public LevelDat(File levelDir)
	{
		this.levelDir=levelDir.toPath();
	}

	/**
	 * Opens the file.
	 * @return returns true if the operation was successful or false if the file doesn't exist or there is another error
	 */
	public boolean open()
	{
		Path levelFile=levelDir.resolve("level.dat");

		if(!Files.exists(levelFile)) return false;

		try (GZIPInputStream stream = new GZIPInputStream(Files.newInputStream(levelFile))) {
			root=(TAG_Compound) NBT_Tag.make(stream);
		} catch (FileNotFoundException e) {
			return false;
		} catch (Exception e) {
			Log.error("Error reading level.dat", e, false);
			return false;
		}

		return true;
	}

	/**
	 * Gets the position of the player.
	 * @return returns a list of X,Y,Z NBT_Float values, or null if the information is not available
	 */
	public TAG_List getPosition()
	{
		TAG_Compound data=(TAG_Compound) root.getElement("Data");
		if (data==null) return null;
		TAG_Int_Array uuid = (TAG_Int_Array) data.getElement("singleplayer_uuid");
		TAG_Compound player;
		if (uuid == null) {
			player = (TAG_Compound) data.getElement("Player");
			if (player == null) return null;
			return (TAG_List) player.getElement("Pos");
		}
		assert uuid.data.length == 4;
		UUID uuid_obj = new UUID(((long) uuid.data[0] << 32) | uuid.data[1] & 0xFFFFFFFFL, ((long) uuid.data[2] << 32) | uuid.data[3] & 0xFFFFFFFFL);
		Path player_dat = levelDir.resolve("players").resolve("data").resolve(uuid_obj + ".dat");
		try (GZIPInputStream stream = new GZIPInputStream(Files.newInputStream(player_dat))){
			player = (TAG_Compound) NBT_Tag.make(stream);
		} catch (FileNotFoundException e) {
			return null;
		} catch (Exception e) {
			Log.error("Error reading " + levelDir.relativize(player_dat), e, false);
			return null;
		}
		return (TAG_List) player.getElement("Pos");
	}

	/**
	 * Gets the X location of the spawn.
	 * @return x coordinate, or 0 if the information is not available
	 */
	public int getSpawnX()
	{
		TAG_Compound data=(TAG_Compound) root.getElement("Data");
		if (data==null) return 0;
		TAG_Int pos = (TAG_Int)data.getElement("SpawnX");
		if (pos==null) return 0;
		return pos.value;
	}

	/**
	 * Gets the Z location of the spawn.
	 * @return z coordinate, or 0 if the information is not available
	 */
	public int getSpawnZ()
	{
		TAG_Compound data=(TAG_Compound) root.getElement("Data");
		if (data==null) return 0;
		TAG_Int pos = (TAG_Int)data.getElement("SpawnZ");
		if (pos==null) return 0;
		return pos.value;
	}

	/**
	 * Prints the description and content of the file into a String.
	 */
	public String toString()
	{
		return "DAT file "+levelDir.toAbsolutePath()+"/level.dat:\n"+root;
	}
}
