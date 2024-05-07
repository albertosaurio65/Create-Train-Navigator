package de.mrjulsen.crn.block;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.mrjulsen.crn.data.EBlockAlignment;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Tripple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AdvancedDisplaySmallBlock extends AbstractAdvancedSidedDisplayBlock {
    
	public static final EnumProperty<EBlockAlignment> Y_ALIGN = EnumProperty.create("y_alignment", EBlockAlignment.class);
	public static final EnumProperty<EBlockAlignment> Z_ALIGN = EnumProperty.create("z_alignment", EBlockAlignment.class);

    private static final Map<ShapeKey, VoxelShape> SHAPES = Map.ofEntries(
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.NEGATIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 8 , 16, 8 , 16)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.NEGATIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 0 , 16, 8 , 8 )),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.NEGATIVE, EBlockAlignment.NEGATIVE), Block.box(8 , 0 , 0 , 16, 8 , 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.NEGATIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 0 , 8 , 8 , 16)),
        
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.CENTER,   EBlockAlignment.NEGATIVE), Block.box(0 , 4 , 8 , 16, 12, 16)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.CENTER,   EBlockAlignment.NEGATIVE), Block.box(0 , 4 , 0 , 16, 12, 8 )),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.CENTER,   EBlockAlignment.NEGATIVE), Block.box(8 , 4 , 0 , 16, 12, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.CENTER,   EBlockAlignment.NEGATIVE), Block.box(0 , 4 , 0 , 8 , 12, 16)),
        
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.POSITIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 8 , 8 , 16, 16, 16)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.POSITIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 8 , 0 , 16, 16, 8 )),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.POSITIVE, EBlockAlignment.NEGATIVE), Block.box(8 , 8 , 0 , 16, 16, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.POSITIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 8 , 0 , 8 , 16, 16)),
        


        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.NEGATIVE, EBlockAlignment.CENTER),   Block.box(0 , 0 , 4 , 16, 8 , 12)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.NEGATIVE, EBlockAlignment.CENTER),   Block.box(0 , 0 , 4 , 16, 8 , 12)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.NEGATIVE, EBlockAlignment.CENTER),   Block.box(4 , 0 , 0 , 12, 8 , 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.NEGATIVE, EBlockAlignment.CENTER),   Block.box(4 , 0 , 0 , 12, 8 , 16)),
        
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.CENTER,   EBlockAlignment.CENTER),   Block.box(0 , 4 , 4 , 16, 12, 12)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.CENTER,   EBlockAlignment.CENTER),   Block.box(0 , 4 , 4 , 16, 12, 12)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.CENTER,   EBlockAlignment.CENTER),   Block.box(4 , 4 , 0 , 12, 12, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.CENTER,   EBlockAlignment.CENTER),   Block.box(4 , 4 , 0 , 12, 12, 16)),
        
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.POSITIVE, EBlockAlignment.CENTER),   Block.box(0 , 8 , 4 , 16, 16, 12)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.POSITIVE, EBlockAlignment.CENTER),   Block.box(0 , 8 , 4 , 16, 16, 12)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.POSITIVE, EBlockAlignment.CENTER),   Block.box(4 , 8 , 0 , 12, 16, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.POSITIVE, EBlockAlignment.CENTER),   Block.box(4 , 8 , 0 , 12, 16, 16)),


        
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.NEGATIVE, EBlockAlignment.POSITIVE), Block.box(0 , 0 , 0 , 16, 8 , 8 )),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.NEGATIVE, EBlockAlignment.POSITIVE), Block.box(0 , 0 , 8 , 16, 8 , 16)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.NEGATIVE, EBlockAlignment.POSITIVE), Block.box(0 , 0 , 0 , 8 , 8 , 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.NEGATIVE, EBlockAlignment.POSITIVE), Block.box(8 , 0 , 0 , 16, 8 , 16)),
        
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.CENTER,   EBlockAlignment.POSITIVE), Block.box(0 , 4 , 0 , 16, 12, 8 )),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.CENTER,   EBlockAlignment.POSITIVE), Block.box(0 , 4 , 8 , 16, 12, 16)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.CENTER,   EBlockAlignment.POSITIVE), Block.box(0 , 4 , 0 , 8 , 12, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.CENTER,   EBlockAlignment.POSITIVE), Block.box(8 , 4 , 0 , 16, 12, 16)),

        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.POSITIVE, EBlockAlignment.POSITIVE), Block.box(0 , 8 , 0 , 16, 16, 8 )),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.POSITIVE, EBlockAlignment.POSITIVE), Block.box(0 , 8 , 8 , 16, 16, 16)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.POSITIVE, EBlockAlignment.POSITIVE), Block.box(0 , 8 , 0 , 8 , 16, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.POSITIVE, EBlockAlignment.POSITIVE), Block.box(8 , 8 , 0 , 16, 16, 16))
    );

    public AdvancedDisplaySmallBlock(Properties properties) {
        super(properties);
		registerDefaultState(defaultBlockState()
            .setValue(Y_ALIGN, EBlockAlignment.CENTER)
            .setValue(Z_ALIGN, EBlockAlignment.CENTER)
        );
    }

    @Override
    public Collection<Property<?>> getExcludedProperties() {
        return List.of(Y_ALIGN, Z_ALIGN);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(new ShapeKey(pState.getValue(FACING), pState.getValue(Y_ALIGN), pState.getValue(Z_ALIGN)));
    }

    @Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		BlockState stateForPlacement = super.getStateForPlacement(pContext);
		Direction direction = pContext.getClickedFace();
        Direction looking = pContext.getHorizontalDirection();
        Axis axis = looking.getAxis();
        AxisDirection axisDirection = looking.getAxisDirection();

        double xzPos = 0.5f;
        if (axis == Axis.X) {
            xzPos = pContext.getClickLocation().x - pContext.getClickedPos().getX();
        } else if (axis == Axis.Z) {            
            xzPos = pContext.getClickLocation().z - pContext.getClickedPos().getZ();
        }

        EBlockAlignment yAlign = EBlockAlignment.CENTER;
        EBlockAlignment zAlign = EBlockAlignment.CENTER;

		if (direction == Direction.UP || (pContext.getClickLocation().y - pContext.getClickedPos().getY() < 0.33333333D)) {
			yAlign = EBlockAlignment.NEGATIVE;
        } else if (direction == Direction.DOWN || (pContext.getClickLocation().y - pContext.getClickedPos().getY() > 0.66666666D)) {
            yAlign = EBlockAlignment.POSITIVE;
        }

        if (direction == pContext.getPlayer().getDirection().getOpposite() || (axisDirection == AxisDirection.POSITIVE ? xzPos > 0.66666666D : xzPos < 0.33333333D)) {
			zAlign = EBlockAlignment.POSITIVE;
        } else if (direction == pContext.getPlayer().getDirection() || (axisDirection == AxisDirection.POSITIVE ? xzPos < 0.33333333D : xzPos > 0.66666666D)) {
            zAlign = EBlockAlignment.NEGATIVE;
        }

		return stateForPlacement
            .setValue(Y_ALIGN, yAlign)
            .setValue(Z_ALIGN, zAlign)
        ;
	}

    @Override
    public boolean canConnectWithBlock(BlockGetter level, BlockPos selfPos, BlockPos otherPos) {
		return super.canConnectWithBlock(level, selfPos, otherPos) &&
            level.getBlockState(selfPos).getValue(Y_ALIGN) == level.getBlockState(otherPos).getValue(Y_ALIGN) && 
            level.getBlockState(selfPos).getValue(Z_ALIGN) == level.getBlockState(otherPos).getValue(Z_ALIGN)
		;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder.add(Y_ALIGN, Z_ALIGN));
	}

    @Override
    public Pair<Float, Float> getRenderAspectRatio(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(1.0F, 0.5F);
    }

    @Override
    public Pair<Float, Float> getRenderOffset(Level level, BlockState blockState, BlockPos pos) {
        float y;
        switch (blockState.getValue(Y_ALIGN)) {
            case NEGATIVE:
                y = 8.0f;
                break;
            case POSITIVE:
                y = 0.0f;
                break;
            default:
                y = 4.0f;
                break;
        }
        return Pair.of(0.0f, y);
    }

    @Override
    public Pair<Float, Float> getRenderZOffset(Level level, BlockState blockState, BlockPos pos) {
        float z1;
        float z2;
        switch (blockState.getValue(Z_ALIGN)) {
            case NEGATIVE:
                z1 = 16.05f;
                z2 = 8.05f;
                break;
            case POSITIVE:
                z1 = 8.05f;
                z2 = 16.05f;
                break;
            default:
                z1 = 12.05f;
                z2 = 12.05f;
                break;
        }
        return Pair.of(z1, z2);
    }

    @Override
    public Tripple<Float, Float, Float> getRenderRotation(Level level, BlockState blockState, BlockPos pos) {
        return Tripple.of(0.0F, 0.0F, 0.0F);
    }

    @Override
    public boolean isSingleLined() {
        return true;
    }

    private static final class ShapeKey {
        private final Direction facing;
        private final EBlockAlignment yAlign;
        private final EBlockAlignment zAlign;
    
        public ShapeKey(Direction facing, EBlockAlignment yAlign, EBlockAlignment zAlign) {
            this.facing = facing;
            this.yAlign = yAlign;
            this.zAlign = zAlign;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ShapeKey other) {
                return facing == other.facing && yAlign == other.yAlign && zAlign == other.zAlign;
            }
            return false;
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(facing, yAlign, zAlign);
        }
    }
}
