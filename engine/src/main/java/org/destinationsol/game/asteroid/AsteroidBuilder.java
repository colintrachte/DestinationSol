/*
 * Copyright 2018 MovingBlocks
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
package org.destinationsol.game.asteroid;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import org.destinationsol.Const;
import org.destinationsol.assets.Assets;
import org.destinationsol.common.SolColor;
import org.destinationsol.common.SolRandom;
import org.destinationsol.game.RemoveController;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.asteroid.procedural.AsteroidShape;
import org.destinationsol.game.asteroid.procedural.RockCollision;
import org.destinationsol.game.asteroid.procedural.RockMesh;
import org.destinationsol.game.drawables.Drawable;
import org.destinationsol.game.drawables.DrawableLevel;
import org.destinationsol.game.drawables.RectSprite;
import org.destinationsol.game.drawables.SpriteManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AsteroidBuilder {
    private static final float DENSITY = 10f;
    private static final float MAX_A_ROT_SPD = .5f;
    private static final float MAX_BALL_SZ = .2f;
    // Each point becomes one Box2D triangle fixture (see RockCollision), so keep this
    // modest - a debris field of these rocks overlapping (e.g. a split/chip burst)
    // pays for fixture-pair narrow-phase cost roughly quadratically in this count.
    private static final int MIN_ROCK_POINTS = 6;
    private static final int MAX_ROCK_POINTS = 9;
    private final List<TextureAtlas.AtlasRegion> textures;
    private final Random rockRandom = new Random();

    @Inject
    public AsteroidBuilder() {
        textures = Assets.listTexturesMatching("engine:asteroid_.*");
    }

    public static Body buildBall(SolGame game, Vector2 position, float angle, float rad, float density, boolean sensor) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.angle = angle * MathUtils.degRad;
        bodyDef.angularDamping = 0;
        bodyDef.position.set(position);
        bodyDef.linearDamping = 0;
        Body body = game.getObjectManager().getWorld().createBody(bodyDef);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = density;
        fixtureDef.friction = Const.FRICTION;
        fixtureDef.restitution = 0.4f;
        fixtureDef.shape = new CircleShape();
        fixtureDef.shape.setRadius(rad);
        fixtureDef.isSensor = sensor;
        body.createFixture(fixtureDef);
        fixtureDef.shape.dispose();
        return body;
    }

    // doesn't consume position
    public Asteroid buildNew(SolGame game, Vector2 position, Vector2 velocity, float size, RemoveController removeController) {
        float rotationSpeed = SolRandom.randomFloat(MAX_A_ROT_SPD);
        return build(game, position, SolRandom.randomElement(textures), size, SolRandom.randomFloat(180), rotationSpeed, velocity, removeController);
    }

    // doesn't consume position
    public FarAsteroid buildNewFar(Vector2 position, Vector2 velocity, float size, RemoveController removeController) {
        float rotationSpeed = SolRandom.randomFloat(MAX_A_ROT_SPD);
        return new FarAsteroid(SolRandom.randomElement(textures), new Vector2(position), SolRandom.randomFloat(180), removeController, size, new Vector2(velocity), rotationSpeed);
    }

    // doesn't consume position
    public Asteroid build(SolGame game, Vector2 position, TextureAtlas.AtlasRegion texture, float size, float angle, float rotationSpeed, Vector2 velocity, RemoveController removeController) {

        ArrayList<Drawable> drawables = new ArrayList<>();
        Body body;
        if (MAX_BALL_SZ < size) {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            bodyDef.angle = angle * MathUtils.degRad;
            bodyDef.angularDamping = 0;
            bodyDef.position.set(position);
            bodyDef.linearDamping = 0;
            body = game.getObjectManager().getWorld().createBody(bodyDef);

            // Procedural silhouette: a jittered-radius perimeter triangulated as a
            // fan from (0,0), so it rotates about its own visual center by
            // construction - no mass-data recentering hack needed.
            int points = SolRandom.randomInt(MIN_ROCK_POINTS, MAX_ROCK_POINTS + 1);
            float variance = SolRandom.randomFloat(.25f, .45f);
            RockMesh mesh = AsteroidShape.generate(size / 2, variance, points, rockRandom);
            RockCollision.attachTriangleFixtures(body, mesh, 1f, DENSITY, Const.FRICTION);

            drawables.add(new ProceduralAsteroidDrawable(mesh, texture, SolColor.WHITE, DrawableLevel.BODIES));
        } else {
            body = buildBall(game, position, angle, size / 2, DENSITY, false);
            RectSprite s = SpriteManager.createSprite(texture.name, size, 0, 0, new Vector2(), DrawableLevel.BODIES, 0, 0, SolColor.WHITE, false);
            drawables.add(s);
        }
        body.setAngularVelocity(rotationSpeed);
        body.setLinearVelocity(velocity);

        Asteroid asteroid = new Asteroid(game, texture, body, size, removeController, drawables);
        body.setUserData(asteroid);
        return asteroid;
    }
}
