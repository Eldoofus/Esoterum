package esoterum.world.blocks.binary;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import esoterum.graphics.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class BinaryNode extends BinaryBlock{
    public int range;
    public float curveWidth = 1.5f;

    public BinaryNode(String name, int linkRange){
        super(name);
        rotate = false;
        emits = true;
        range = linkRange;
        configurable = true;

        outputs = new boolean[]{true, true, true, true};
        inputs = new boolean[]{true, true, true, true};

        //point2 config is relative
        config(Point2.class, (BinaryNodeBuild tile, Point2 i) -> tile.link = Point2.pack(i.x + tile.tileX(), i.y + tile.tileY()));
        //integer is not
        config(Integer.class, (BinaryNodeBuild tile, Integer i) -> tile.link = i);

        configClear((BinaryNodeBuild tile) -> tile.link = -1);
    }

    public BinaryNode(String name){
        this(name, 20);
    }

    @Override
    public void init(){
        super.init();

        clipSize = Math.max(clipSize, (range * tilesize + 8f) * 2f);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.circles(x * tilesize + offset, y * tilesize + offset, range * tilesize, Color.white);
        Draw.reset();
    }

    public boolean linkValid(Tile tile, Tile other){
        if(other == null || tile == null || other.dst(tile) > range * tilesize) return false;

        return other.block() == tile.block() && tile.team() == other.team();
    }

    public BinaryNodeBuild getLink(int pos){
        return world.build(pos) instanceof BinaryNodeBuild b ? b : null;
    }

    public class BinaryNodeBuild extends BinaryBuild{
        public int link = -1;

        @Override
        public void updateTile(){
            super.updateTile();
            BinaryNodeBuild c = linkedNode();
            if(c != null && c.link != pos()){
                configure(null);
            }
        }
        @Override
        public void updateSignal(int source){
            try {
                super.updateSignal(source);
                boolean tmp = signal[4];
                signal[4] = false;
                for(BinaryBuild b : nb){
                    signal[4] |= getSignal(b, this);
                }
                BinaryNodeBuild c = linkedNode();
                if(tmp != signal[4]){
                    tmp = signal[4];
                    try {if(c != null && source != 4) c.updateSignal(4);} catch(Exception e){}
                }
                signal[4] = c != null && c.signal();
                if(signal[0] != signal[4]){
                    signal(signal[4]);
                    propagateSignal(source != 0, source != 1, source != 2, source != 3);
                }
                signal[4] = tmp;
            } catch(Exception e){}
        }

        @Override
        public void placed(){
            super.placed();

            BinaryNodeBuild c = linkedNode();
            if(c != null && c.link != pos()){
                c.disconnect();
                c.configure(pos());
            }
        }

        @Override
        public void draw(){
            if(!rotate || !rotatedBase){
                Draw.rect(region, x, y);
            } else {
                Draw.rect(baseRegions[rotation], x, y);
            }

            drawConnections();
            Draw.color(Color.white, Pal.accent, signal[0] ? 1f : 0f);
            Draw.rect(topRegion, x, y, (rotate && drawRot) ? rotdeg() : 0f);

            BinaryNodeBuild c = linkedNode();
            if(c != null){
                Draw.z(Layer.power);
                Lines.stroke(1f);
                Draw.color(Color.white, Pal.accent, signal() ? 1f : 0f);
                EsoDrawf.curvedLine(x, y, c.x, c.y, -curveWidth); //Negative so that it goes clockwise if positive

                float time = (Time.time / 60f) % 3f;
                Tmp.v1.trns(angleTo(c) - 90f, Mathf.sin(time / 3f * Mathf.PI) * -curveWidth, Mathf.lerp(0f, dst(c), time / 3f));
                Fill.circle(
                    x + Tmp.v1.x,
                    y + Tmp.v1.y,
                    1.5f
                );
                Draw.reset();
            }
        }

        @Override
        public boolean signal(){
            return signal[4];
        }

        @Override
        public void drawConfigure(){
            Tmp.c1.set(Color.white).lerp(Pal.accent, signal() ? 1f : 0f);

            Drawf.circles(x, y, size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f), Tmp.c1);
            Drawf.circles(x, y, range * tilesize, Tmp.c1);

            BinaryNodeBuild connection = linkedNode();
            if(link != -1 && connection != null){
                Drawf.square(connection.x, connection.y, connection.block.size * tilesize / 2f + 1f, Tmp.c1);
            }
        }

        @Override
        public boolean onConfigureTileTapped(Building other){
            if(linkValid(tile, other.tile)){
                disconnect();
                if(other.pos() == link){
                    configure(null);
                } else if(other != self()){
                    ((BinaryNodeBuild)other).disconnect();
                    getLink(other.pos()).configure(pos());
                    configure(other.pos());
                }
                return false;
            }
            return true;
        }

        public void disconnect(){
            if(linkedNode() != null){
                linkedNode().configure(null);
            }
            configure(null);
        }

        @Override
        public void remove(){
            super.remove();
            disconnect();
        }

        @Override
        public Point2 config(){
            return Point2.unpack(link).sub(tile.x, tile.y);
        }

        public BinaryNodeBuild linkedNode(){
            return getLink(link);
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(link);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, (byte)(revision + 1));

            link = read.i();
        }
    }
}
