package com.poixson.backrooms.gens;

import static com.poixson.backrooms.worlds.Level_000.ENABLE_GEN_309;
import static com.poixson.backrooms.worlds.Level_000.SUBFLOOR;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.poixson.backrooms.BackroomsGen;
import com.poixson.backrooms.BackroomsLevel;
import com.poixson.backrooms.PreGenData;
import com.poixson.commonmc.tools.PathTracer;
import com.poixson.commonmc.tools.plotter.BlockPlotter;
import com.poixson.utils.FastNoiseLiteD;
import com.poixson.utils.FastNoiseLiteD.FractalType;


// 309 | Radio Station
public class Gen_309 extends BackroomsGen {

	// default params
	public static final int DEFAULT_PATH_WIDTH    = 3;
	public static final int DEFAULT_PATH_CLEARING = 10;
	public static final int PATH_START_X  = 14;
	public static final int PATH_START_Z  = 32;

	// default blocks
	public static final String DEFAULT_BLOCK_DIRT       = "minecraft:dirt";
	public static final String DEFAULT_BLOCK_PATH       = "minecraft:dirt_path";
	public static final String DEFAULT_BLOCK_GRASS      = "minecraft:grass_block";
	public static final String DEFAULT_BLOCK_SUBFLOOR   = "minecraft:stone";
	public static final String DEFAULT_BLOCK_TREETRUNK  = "minecraft:birch_log";
	public static final String DEFAULT_BLOCK_TREELEAVES = "minecraft:birch_leaves";

	// noise
	public final FastNoiseLiteD noisePath;
	public final FastNoiseLiteD noisePathGround;
	public final FastNoiseLiteD noiseTrees;

	// params
	public final AtomicInteger path_width    = new AtomicInteger(DEFAULT_PATH_WIDTH   );
	public final AtomicInteger path_clearing = new AtomicInteger(DEFAULT_PATH_CLEARING);

	// path locations
	protected final PathTracer pathTrace;
	protected final AtomicReference<ConcurrentHashMap<Integer, Double>> pathCache =
			new AtomicReference<ConcurrentHashMap<Integer, Double>>(null);

	// blocks
	public final AtomicReference<String> block_dirt        = new AtomicReference<String>(null);
	public final AtomicReference<String> block_path        = new AtomicReference<String>(null);
	public final AtomicReference<String> block_grass       = new AtomicReference<String>(null);
	public final AtomicReference<String> block_subfloor    = new AtomicReference<String>(null);
	public final AtomicReference<String> block_tree_trunk  = new AtomicReference<String>(null);
	public final AtomicReference<String> block_tree_leaves = new AtomicReference<String>(null);



	public Gen_309(final BackroomsLevel backlevel,
			final int level_y, final int level_h) {
		super(backlevel, level_y, level_h);
		// path
		this.noisePath = this.register(new FastNoiseLiteD());
		this.noisePath.setFrequency(0.01f);
		// path ground
		this.noisePathGround = this.register(new FastNoiseLiteD());
		this.noisePathGround.setFrequency(0.002f);
		this.noisePathGround.setFractalType(FractalType.Ridged);
		this.noisePathGround.setFractalOctaves(3);
		this.noisePathGround.setFractalGain(0.5f);
		this.noisePathGround.setFractalLacunarity(2.0f);
		// tree noise
		this.noiseTrees = this.register(new FastNoiseLiteD());
		this.noiseTrees.setFrequency(0.2f);
		// path locations
		this.pathTrace = new PathTracer(this.noisePath, PATH_START_X, PATH_START_Z, this.getPathCacheMap());
	}



	@Override
	public void unregister() {
		super.unregister();
		this.pathCache.set(null);
	}



	public FastNoiseLiteD getTreeNoise() {
		return this.noiseTrees;
	}



	@Override
	public void generate(final PreGenData pregen, final ChunkData chunk,
			final LinkedList<BlockPlotter> plots, final int chunkX, final int chunkZ) {
		if (!ENABLE_GEN_309) return;
		final int path_width = this.path_width.get();
		final BlockData block_dirt     = StringToBlockData(this.block_dirt,     DEFAULT_BLOCK_DIRT    );
		final BlockData block_path     = StringToBlockData(this.block_path,     DEFAULT_BLOCK_PATH    );
		final BlockData block_grass    = StringToBlockData(this.block_grass,    DEFAULT_BLOCK_GRASS   );
		final BlockData block_subfloor = StringToBlockData(this.block_subfloor, DEFAULT_BLOCK_SUBFLOOR);
		if (block_dirt     == null) throw new RuntimeException("Invalid block type for level 309 Dirt"    );
		if (block_path     == null) throw new RuntimeException("Invalid block type for level 309 Path"    );
		if (block_grass    == null) throw new RuntimeException("Invalid block type for level 309 Grass"   );
		if (block_subfloor == null) throw new RuntimeException("Invalid block type for level 309 SubFloor");
		final int y = this.level_y + SUBFLOOR + 1;
		for (int iz=0; iz<16; iz++) {
			for (int ix=0; ix<16; ix++) {
				final int xx = (chunkX * 16) + ix;
				final int zz = (chunkZ * 16) + iz;
				chunk.setBlock(ix, this.level_y, iz, Material.BEDROCK);
				for (int iy=0; iy<SUBFLOOR; iy++)
					chunk.setBlock(ix, this.level_y+iy+1, iz, block_subfloor);
				final double ground;
				{
					final double g = this.noisePathGround.getNoise(xx, zz);
					ground = 1.0f + (g < 0.0f ? g * 0.6f : g);
				}
				// dirt
				final int elevation = (int) (ground * 2.5f); // 0 to 5
				for (int i=0; i<elevation; i++) {
					if (i >= elevation-1) {
						if (this.pathTrace.isPath(xx, zz, path_width)) {
							chunk.setBlock(ix, y+i, iz, block_path);
						} else {
							chunk.setBlock(ix, y+i, iz, block_grass);
						}
					} else {
						chunk.setBlock(ix, y+i, iz, block_dirt);
					}
				}
			} // end ix
		} // end iz
	}



	public int getPathX(final int z) {
		if (z < 0) return 0;
		return this.pathTrace.getPathX(z);
	}



	public ConcurrentHashMap<Integer, Double> getPathCacheMap() {
		// existing
		{
			final ConcurrentHashMap<Integer, Double> cache = this.pathCache.get();
			if (cache != null)
				return cache;
		}
		// new instance
		{
			final ConcurrentHashMap<Integer, Double> cache = new ConcurrentHashMap<Integer, Double>();
			if (this.pathCache.compareAndSet(null, cache))
				return cache;
		}
		return this.getPathCacheMap();
	}



	public double getCenterClearingDistance(final int x, final int z, final double strength) {
		if (Math.abs(x) > 100 || Math.abs(z) > 100)
			return Double.MAX_VALUE;
		return Math.sqrt( Math.pow((double)x, 2.0) + Math.pow((double)z, 2.0) )
			+ (this.noisePath.getNoise(x*5, z*5) * strength);
	}



	// -------------------------------------------------------------------------------
	// configs



	@Override
	protected void loadConfig() {
		// params
		{
			final ConfigurationSection cfg = this.plugin.getLevelParams(309);
			this.path_width   .set(cfg.getInt("Path-Width"   ));
			this.path_clearing.set(cfg.getInt("Path-Clearing"));
		}
		// block types
		{
			final ConfigurationSection cfg = this.plugin.getLevelBlocks(309);
			this.block_tree_trunk .set(cfg.getString("TreeTrunk" ));
			this.block_tree_leaves.set(cfg.getString("TreeLeaves"));
		}
	}
	public static void ConfigDefaults(final FileConfiguration cfg) {
		// params
		cfg.addDefault("Level309.Params.Path-Width",    DEFAULT_PATH_WIDTH   );
		cfg.addDefault("Level309.Params.Path-Clearing", DEFAULT_PATH_CLEARING);
		// block types
		cfg.addDefault("Level309.Blocks.Dirt",       DEFAULT_BLOCK_DIRT      );
		cfg.addDefault("Level309.Blocks.Path",       DEFAULT_BLOCK_PATH      );
		cfg.addDefault("Level309.Blocks.Grass",      DEFAULT_BLOCK_GRASS     );
		cfg.addDefault("Level309.Blocks.SubFloor",   DEFAULT_BLOCK_SUBFLOOR  );
		cfg.addDefault("Level309.Blocks.TreeTrunk",  DEFAULT_BLOCK_TREETRUNK );
		cfg.addDefault("Level309.Blocks.TreeLeaves", DEFAULT_BLOCK_TREELEAVES);
	}



}
