package com.poixson.backrooms.levels;

import static com.poixson.backrooms.levels.Level_000.SUBCEILING;
import static com.poixson.backrooms.levels.Level_000.SUBFLOOR;

import java.util.LinkedList;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.poixson.commonmc.tools.plotter.BlockPlotter;
import com.poixson.utils.FastNoiseLiteD;
import com.poixson.utils.FastNoiseLiteD.CellularDistanceFunction;
import com.poixson.utils.FastNoiseLiteD.FractalType;
import com.poixson.utils.FastNoiseLiteD.NoiseType;


// 19 | Attic
public class Gen_019 extends GenBackrooms {

	public static final boolean ENABLE_GENERATE = true;
	public static final boolean ENABLE_ROOF     = true;

	public static final Material ATTIC_FLOOR = Material.SPRUCE_PLANKS;
	public static final Material ATTIC_WALLS = Material.SPRUCE_PLANKS;

	public final int level_m;

	// noise
	public final FastNoiseLiteD noiseAtticWalls;



	public Gen_019(final LevelBackrooms backlevel,
			final int level_y, final int level_h) {
		super(backlevel, level_y, level_h);
		this.level_m = Math.floorDiv(this.level_h, 2);
		// attic walls
		this.noiseAtticWalls = this.register(new FastNoiseLiteD());
		this.noiseAtticWalls.setFrequency(0.02);
		this.noiseAtticWalls.setNoiseType(NoiseType.Cellular);
		this.noiseAtticWalls.setFractalType(FractalType.PingPong);
		this.noiseAtticWalls.setCellularDistanceFunction(CellularDistanceFunction.Manhattan);
	}



	@Override
	public void generate(final PreGenData pregen, final ChunkData chunk,
			final LinkedList<BlockPlotter> plots, final int chunkX, final int chunkZ) {
		if (!ENABLE_GENERATE) return;
		final int y  = this.level_y + SUBFLOOR + 1;
		final int cy = this.level_y + SUBFLOOR + this.level_h + 1;
		for (int z=0; z<16; z++) {
			for (int x=0; x<16; x++) {
				final int xx = (chunkX * 16) + x;
				final int zz = (chunkZ * 16) + z;
				// lobby floor
				chunk.setBlock(x, this.level_y, z, Material.BEDROCK);
				for (int yy=0; yy<SUBFLOOR; yy++) {
					chunk.setBlock(x, this.level_y+yy+1, z, ATTIC_FLOOR);
				}
				final double value = this.noiseAtticWalls.getNoiseRot(xx, zz, 0.25);
				if (value < -0.9 || value > 0.9) {
					for (int iy=0; iy<3; iy++) {
						chunk.setBlock(x, y+iy, z, ATTIC_WALLS);
					}
				}
				// second floor
				chunk.setBlock(x, y+this.level_m, z, ATTIC_WALLS);
				if (ENABLE_ROOF) {
					for (int i=0; i<SUBCEILING; i++) {
						chunk.setBlock(x, cy+i+1, z, ATTIC_FLOOR);
					}
				}
			} // end x
		} // end z
	}



}
