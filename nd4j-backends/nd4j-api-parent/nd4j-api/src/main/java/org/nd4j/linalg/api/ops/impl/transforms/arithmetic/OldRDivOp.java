/*-
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 *
 */

package org.nd4j.linalg.api.ops.impl.transforms.arithmetic;

import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseTransformOp;

import java.util.ArrayList;
import java.util.List;

/**
 * Reverse Division operation
 *
 * @author Adam Gibson
 */
public class OldRDivOp extends BaseTransformOp {
    public OldRDivOp(SameDiff sameDiff, SDVariable i_v1, SDVariable i_v2) {
        super(sameDiff, i_v1, i_v2);
    }

    public OldRDivOp(SameDiff sameDiff, SDVariable i_v1, SDVariable i_v2, boolean inPlace) {
        super(sameDiff, i_v1, i_v2, inPlace);
    }

    public OldRDivOp() {}

    public OldRDivOp(INDArray x, INDArray y, INDArray z, long n) {
        super(x, y, z, n);
    }

    public OldRDivOp(INDArray x) {
        super(x);
    }

    public OldRDivOp(INDArray x, INDArray z) {
        super(x, z);
    }

    public OldRDivOp(INDArray x, INDArray z, long n) {
        super(x, z, n);
    }

    public OldRDivOp(INDArray x, INDArray y, INDArray z) {
        super(x, y, z, x.lengthLong());
    }

    @Override
    public int opNum() {
        return 18;
    }

    @Override
    public String opName() {
        return "rdiv";
    }

    @Override
    public String onnxName() {
        return "Div";
    }

    @Override
    public String tensorflowName() {
        return "div";
    }



    @Override
    public void init(INDArray x, INDArray y, INDArray z, long n) {
        super.init(x, y, z, n);
        if (y == null)
            throw new IllegalArgumentException("No components to divide");
    }




    @Override
    public List<SDVariable> doDiff(List<SDVariable> i_v) {
        SDVariable gradWrtX = f().div(i_v.get(0),larg());
        SDVariable gradWrtY = f().mul(f().neg(gradWrtX),f().div(rarg(),larg()));
        List<SDVariable> ret = new ArrayList<>(2);
        ret.add(gradWrtX);
        ret.add(gradWrtY);
        return ret;
    }

}
