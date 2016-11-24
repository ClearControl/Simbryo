package embryosim.viewer.three.demo;

import org.junit.Test;

import embryosim.psystem.ParticleSystem;
import embryosim.psystem.forcefield.interaction.impl.CollisionForceField;
import embryosim.util.jfx.JavaFXUtil;
import embryosim.util.timing.Timming;
import embryosim.viewer.three.ParticleViewer3D;
import embryosim.viewer.two.ParticleViewer2D;

public class ParticleViewer3DDemo
{

  @Test
  public void demo3D() throws InterruptedException
  {
    int G = 16;
    int N = 1200;
    float V = 0.0001f;
    float R = (float) (0.2 / Math.pow(N, 0.33f));
    float Rm = 0.01f;
    float D = 0.9999f;
    float Db = 0.9f;
    float Fc = 0.00001f;
    float Fg = 0.000001f;

    CollisionForceField lCollisionForceField =
                                             new CollisionForceField(Fc,
                                                                     D);

    ParticleSystem lParticleSystem = new ParticleSystem(3,
                                                        N,
                                                        Rm,
                                                        Rm + 0.5f
                                                             * R);

    for (int i = 0; i < N; i++)
    {
      float x = (float) Math.random();
      float y = (float) Math.random();
      float z = (float) Math.random();

      lParticleSystem.addParticle(x, y, z);
      lParticleSystem.setVelocity(i,
                                  (float) (V
                                           * (Math.random() - 0.5f)),
                                  (float) (V
                                           * (Math.random() - 0.5f)),
                                  (float) (V
                                           * (Math.random() - 0.5f)));
      lParticleSystem.setRadius(i, (float) (Rm + (R))); // Math.random() *
    }

    lParticleSystem.setRadius(0, 0.06f);
    // lParticleSystem.setRadius(1, 0.06f);
    // lParticleSystem.setPosition(1, 0.55f, 0.45f);

    lParticleSystem.updateNeighborhoodCells();

    // System.out.println(Arrays.toString(lParticleSystem.getVelocities()));

    ParticleViewer3D lParticleViewer3D =
                                       ParticleViewer3D.view(lParticleSystem,
                                                             "Particles Are Fun",
                                                             768,
                                                             768);

    Timming lTimming = new Timming();

    while (lParticleViewer3D.isShowing())
    {
      lTimming.syncAtPeriod(3);

      // lParticleSystem.repelAround(lMouseX, lMouseY, 0.00001f);
      lParticleSystem.applyForceField(lCollisionForceField);
      if (Fg > 0)
        lParticleSystem.applyForce(0f, Fg, 0f);
      lParticleSystem.intergrateEuler();
      lParticleSystem.enforceBounds(Db);
      lParticleSystem.updateNeighborhoodCells();

      /*float lMouseX = (float) (lParticleViewer3D.getMouseX());
      float lMouseY = (float) (lParticleViewer3D.getMouseY());/**/

      // lParticleSystem.setPosition(0, lMouseX, lMouseY);

      lParticleViewer3D.updateDisplay(true);
    }

    lParticleViewer3D.waitWhileShowing();
  }

}
