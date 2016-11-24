package embryosim.embryo.zoo;

import embryosim.embryo.EmbryoBase;
import embryosim.forcefield.external.impl.CentriForceField;

public class Organoid extends EmbryoBase
{

  private static final float Fpetal = 0.00005f;
  private static final float Ri = 0.135f;
  private CentriForceField mCentriForceField;
  

  public Organoid(int pInicialNumberOfcells)
  {
    super(3, pInicialNumberOfcells, Ri);
    
    mRadiusShrinkageFactor = (float) Math.pow(0.5f, 1.0f / 4);
    
    mCentriForceField = new CentriForceField(Fpetal, 0.5f, 0.5f, 0.5f);
  }

  @Override
  public void simulationSteps(int pNumberOfSteps)
  {
    for (int i = 0; i < pNumberOfSteps; i++)
    {
      applyForceField(mCentriForceField);
      super.simulationSteps(1);
    }
  }


}
