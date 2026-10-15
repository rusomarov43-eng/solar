package dev.burmalda;

import net.minecraft.client.model.*;

public final class HatModel {
    public static final String[] IDS = {"tophat", "crown", "halo", "party", "horns", "cap", "cat", "bunny", "wizard", "phones", "antenna"};

    // Model space: y grows DOWN, head cuboid spans y -8..0, back of the body is +z.
    public static TexturedModelData createLayer() {
        ModelData d = new ModelData();
        ModelPartData r = d.getRoot();

        r.addChild("tophat", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-5f, -9f, -5f, 10f, 1f, 10f)
                .cuboid(-3.5f, -17f, -3.5f, 7f, 8f, 7f), ModelTransform.NONE);

        r.addChild("crown", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4.5f, -10f, -4.5f, 9f, 2f, 9f)
                .cuboid(-4.5f, -13f, -4.5f, 2f, 3f, 2f)
                .cuboid(2.5f, -13f, -4.5f, 2f, 3f, 2f)
                .cuboid(-4.5f, -13f, 2.5f, 2f, 3f, 2f)
                .cuboid(2.5f, -13f, 2.5f, 2f, 3f, 2f)
                .cuboid(-1f, -12f, -4.5f, 2f, 2f, 2f)
                .cuboid(-1f, -12f, 2.5f, 2f, 2f, 2f), ModelTransform.NONE);

        r.addChild("halo", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-5f, -13f, -5f, 10f, 1f, 1f)
                .cuboid(-5f, -13f, 4f, 10f, 1f, 1f)
                .cuboid(-5f, -13f, -4f, 1f, 1f, 8f)
                .cuboid(4f, -13f, -4f, 1f, 1f, 8f), ModelTransform.NONE);

        r.addChild("party", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4f, -10f, -4f, 8f, 2f, 8f)
                .cuboid(-3f, -12f, -3f, 6f, 2f, 6f)
                .cuboid(-2f, -14f, -2f, 4f, 2f, 4f)
                .cuboid(-1f, -16f, -1f, 2f, 2f, 2f), ModelTransform.NONE);

        r.addChild("horns", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-5.5f, -10f, -1f, 2f, 2f, 2f)
                .cuboid(-6.5f, -12.5f, -0.5f, 1f, 3f, 1f)
                .cuboid(3.5f, -10f, -1f, 2f, 2f, 2f)
                .cuboid(5.5f, -12.5f, -0.5f, 1f, 3f, 1f), ModelTransform.NONE);

        r.addChild("cap", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4.5f, -9.5f, -4.5f, 9f, 2f, 9f)
                .cuboid(-4.5f, -8f, -8f, 9f, 0.5f, 3.5f), ModelTransform.NONE);

        r.addChild("cat", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4f, -10f, -2f, 2f, 2f, 2f)
                .cuboid(-3.5f, -11f, -1.5f, 1f, 1f, 1f)
                .cuboid(2f, -10f, -2f, 2f, 2f, 2f)
                .cuboid(2.5f, -11f, -1.5f, 1f, 1f, 1f), ModelTransform.NONE);

        r.addChild("bunny", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-3.5f, -17f, -1f, 2f, 9f, 1f)
                .cuboid(1.5f, -17f, -1f, 2f, 9f, 1f), ModelTransform.NONE);

        r.addChild("wizard", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-6f, -9f, -6f, 12f, 1f, 12f)
                .cuboid(-4f, -12f, -4f, 8f, 3f, 8f)
                .cuboid(-3f, -15f, -3f, 6f, 3f, 6f)
                .cuboid(-2f, -18f, -2f, 4f, 3f, 4f)
                .cuboid(-1f, -21f, -1f, 2f, 3f, 2f), ModelTransform.NONE);

        r.addChild("phones", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4.5f, -10f, -1f, 9f, 1.5f, 2f)
                .cuboid(-5.5f, -9f, -1f, 1f, 5f, 2f)
                .cuboid(4.5f, -9f, -1f, 1f, 5f, 2f)
                .cuboid(-7f, -5f, -2.5f, 2f, 5f, 5f)
                .cuboid(5f, -5f, -2.5f, 2f, 5f, 5f), ModelTransform.NONE);

        r.addChild("antenna", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-0.5f, -12f, -0.5f, 1f, 4f, 1f)
                .cuboid(-1.5f, -14.5f, -1.5f, 3f, 3f, 3f), ModelTransform.NONE);

        // cape: hangs from the shoulders, extends backwards (+z) after the 180 deg flip
        r.addChild("cape", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-5f, 0f, -1f, 10f, 16f, 1f), ModelTransform.NONE);

        // wings: arm + 3 feathers each, pivot at the upper back
        ModelPartData wl = r.addChild("wing_l", ModelPartBuilder.create().uv(0, 0)
                .cuboid(0f, -1f, 0f, 9f, 2f, 1f), ModelTransform.pivot(2f, 2f, 2f));
        wl.addChild("wl1", ModelPartBuilder.create().uv(0, 0).cuboid(0f, 0f, 0f, 2f, 10f, 1f), ModelTransform.pivot(2f, 1f, 0f));
        wl.addChild("wl2", ModelPartBuilder.create().uv(0, 0).cuboid(0f, 0f, 0f, 2f, 13f, 1f), ModelTransform.pivot(4.5f, 1f, 0f));
        wl.addChild("wl3", ModelPartBuilder.create().uv(0, 0).cuboid(0f, 0f, 0f, 2f, 9f, 1f), ModelTransform.pivot(7f, 1f, 0f));

        ModelPartData wr = r.addChild("wing_r", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-9f, -1f, 0f, 9f, 2f, 1f), ModelTransform.pivot(-2f, 2f, 2f));
        wr.addChild("wr1", ModelPartBuilder.create().uv(0, 0).cuboid(-2f, 0f, 0f, 2f, 10f, 1f), ModelTransform.pivot(-2f, 1f, 0f));
        wr.addChild("wr2", ModelPartBuilder.create().uv(0, 0).cuboid(-2f, 0f, 0f, 2f, 13f, 1f), ModelTransform.pivot(-4.5f, 1f, 0f));
        wr.addChild("wr3", ModelPartBuilder.create().uv(0, 0).cuboid(-2f, 0f, 0f, 2f, 9f, 1f), ModelTransform.pivot(-7f, 1f, 0f));

        return TexturedModelData.of(d, 16, 16);
    }

    private HatModel() { }
}
