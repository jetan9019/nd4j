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

package org.nd4j.linalg.api.ops.impl.accum;

import com.google.common.primitives.Ints;
import lombok.NoArgsConstructor;
import lombok.val;
import onnx.OnnxProto3;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.blas.params.MMulTranspose;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.linalg.api.ops.Op;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.util.ArrayUtil;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.nd4j.linalg.util.ArrayUtil.*;

/**
 * TensorMmul
 * @author Adam Gibson
 */
@NoArgsConstructor
public class TensorMmul extends DynamicCustomOp {
    private int[][] axes;
    protected boolean addedEdges;
    protected MMulTranspose mMulTranspose;

    public TensorMmul(SameDiff sameDiff,
                      SDVariable i_v1,
                      SDVariable i_v2,
                      int[][] dimensions) {
        this(sameDiff,i_v1,i_v2,dimensions,MMulTranspose.allFalse());
    }

    public TensorMmul(SameDiff sameDiff,
                      SDVariable i_v1,
                      SDVariable i_v2,
                      int[][] dimensions,
                      MMulTranspose mMulTranspose) {
        this.sameDiff = sameDiff;
        this.mMulTranspose = mMulTranspose;
        this.axes = dimensions;
        this.extraArgs = new Object[] {axes,mMulTranspose};
        sameDiff.addArgsFor(new SDVariable[] {i_v1,i_v2},this);
        val vertexId = outputVariables()[0].getVertexId();
        if(!addedEdges) {
            sameDiff.addOutgoingFor(new int[]{vertexId},this);
            addedEdges = true;
        }
    }

    @Override
    public List<int[]> calculateOutputShape() {
        List<int[]> ret = new ArrayList<>(1);
        int[] aShape = mMulTranspose.isTransposeA() ? ArrayUtil.reverseCopy(larg().getShape()) : larg().getShape();
        int[] bShape = mMulTranspose.isTransposeB() ? ArrayUtil.reverseCopy(rarg().getShape()) : rarg().getShape();
        if(aShape != null && bShape != null) {
            val shape =  this instanceof Mmul ? Shape.getMatrixMultiplyShape(
                    aShape,bShape)
                    : getTensorMmulShape(aShape,bShape, axes);
            ret.add(shape);
        }
        if(!ret.isEmpty()) {
            for(int i = 0; i < ret.get(0).length; i++) {
                if(ret.get(0)[i] < 1)
                    throw new ND4JIllegalStateException("Invalid shape computed at index " +  i);
            }
        }
        return ret;
    }

    @Override
    public List<SDVariable> doDiff(List<SDVariable> i_v1) {
        List<SDVariable> ret = new ArrayList<>();
        int[] bAxes = range(0, rarg().getShape().length);
        int[] aAxes = range(0, larg().getShape().length);
        int aRank = larg().getShape().length;
        int bRank = rarg().getShape().length;
        int[][] sumAxes = new int[][]{
                mod(axes[0], aRank), mod(axes[1], bRank)
        };
        int[][] deletedAxes = new int[][]{
                removeIndex(aAxes, sumAxes[0]),
                removeIndex(bAxes, sumAxes[1])};
        int[] gAxes = range(0, i_v1.get(0).getShape().length);
        int[][] firstAxes = new int[][]{
                Arrays.copyOfRange(gAxes, deletedAxes[0].length, gAxes.length),
                deletedAxes[1]
        };

        int[][] secondAxes = new int[][]{
                deletedAxes[0],
                Arrays.copyOfRange(gAxes, 0, deletedAxes[0].length)
        };


        //tensor matrix multiply gradient wrt second variable
        int[] firstPerm = argsort(combine(deletedAxes[0],keep(argsort(sumAxes[1]),sumAxes[0])));
        SDVariable firstResult = doTensorMmul(i_v1.get(0), rarg(), firstAxes);
        SDVariable permuted = f().permute(firstResult,firstPerm);
        ret.add(permuted);

        //tensor matrix multiply gradient wrt first variable
        int[] secondPerm = argsort(combine(keep(argsort(sumAxes[0]),sumAxes[1]),deletedAxes[1]));
        SDVariable secondResult = doTensorMmul(i_v1.get(0), larg(), secondAxes);
        SDVariable secondPermuted = f().permute(secondResult,secondPerm);
        ret.add(secondPermuted);
        return ret;
    }



    private SDVariable doTensorMmul(SDVariable a,
                                    SDVariable b,
                                    int[][] axes) {

        int validationLength = Math.min(axes[0].length, axes[1].length);
        for (int i = 0; i < validationLength; i++) {
            if (a.getShape()[axes[0][i]] != b.getShape()[axes[1][i]])
                throw new IllegalArgumentException("Size of the given axes at each dimension must be the same size.");
            if (axes[0][i] < 0)
                axes[0][i] += a.getShape().length;
            if (axes[1][i] < 0)
                axes[1][i] += b.getShape().length;

        }

        List<Integer> listA = new ArrayList<>();
        for (int i = 0; i < a.getShape().length; i++) {
            if (!Ints.contains(axes[0], i))
                listA.add(i);
        }

        int[] newAxesA = Ints.concat(Ints.toArray(listA), axes[0]);


        List<Integer> listB = new ArrayList<>();
        for (int i = 0; i < b.getShape().length; i++) {
            if (!Ints.contains(axes[1], i))
                listB.add(i);
        }

        int[] newAxesB = Ints.concat(axes[1], Ints.toArray(listB));

        int n2 = 1;
        int aLength = Math.min(a.getShape().length, axes[0].length);
        for (int i = 0; i < aLength; i++) {
            n2 *= a.getShape()[axes[0][i]];
        }

        //if listA and listB are empty these do not initialize.
        //so initializing with {1} which will then get overridden if not empty
        int[] newShapeA = {-1, n2};
        int[] oldShapeA;
        if (listA.size() == 0) {
            oldShapeA = new int[] {1};
        } else {
            oldShapeA = Ints.toArray(listA);
            for (int i = 0; i < oldShapeA.length; i++)
                oldShapeA[i] = a.getShape()[oldShapeA[i]];
        }

        int n3 = 1;
        int bNax = Math.min(b.getShape().length, axes[1].length);
        for (int i = 0; i < bNax; i++) {
            n3 *= b.getShape()[axes[1][i]];
        }


        int[] newShapeB = {n3, -1};
        int[] oldShapeB;
        if (listB.size() == 0) {
            oldShapeB = new int[] {1};
        } else {
            oldShapeB = Ints.toArray(listB);
            for (int i = 0; i < oldShapeB.length; i++)
                oldShapeB[i] = b.getShape()[oldShapeB[i]];
        }


        SDVariable at = f()
                .reshape(f().permute
                        (a,newAxesA),newShapeA);
        SDVariable bt = f()
                .reshape(f()
                        .permute(b,newAxesB),newShapeB);

        SDVariable ret = f().mmul(at,bt);
        int[] aPlusB = Ints.concat(oldShapeA, oldShapeB);
        return f().reshape(ret,aPlusB);
    }


    public TensorMmul(INDArray x, INDArray y, int[][] axes) {
        super(null,new INDArray[]{x, y},null);
        this.axes = axes;
        this.extraArgs = new Object[] {axes};
    }

    /**
     * Initialize with the given
     * input, pairwise transform, result, and number
     * of elements
     *
     * @param x the input
     * @param y the pairwise transform
     * @param z the result
     */
    public TensorMmul(INDArray x, INDArray y, INDArray z, int[][] axes) {
        super(null,new INDArray[]{x, y, z},null);
        this.axes = axes;
    }


    @Override
    public int opNum() {
        return 3;
    }

    @Override
    public String opName() {
        return "tensormmul";
    }


    @Override
    public void initWithArrays(Map<String, INDArray> arrayMap, Object... extraArgs) {
        if (isArrayInit() || isArrayInitialized())
            return;


        super.initWithArrays(arrayMap);
        for (int i = 0; i < args().length; i++) {
            if (args()[i].getShape() == null) {
                throw new ND4JIllegalStateException("Unable to get shape for arg " + i);
            }
        }

        val outputVertexId = outputVariables()[0].getVertexId();

        val var = sameDiff.getVariableForVertexId(outputVertexId);
        INDArray arr = sameDiff.getArrForVertexId(var.getVertexId());
        if (arr == null) {
            arr = var.getWeightInitScheme().create(calculateOutputShape().get(0));
            sameDiff.putArrayForVertexId(outputVertexId, arr);
            addOutputArgument(arr);
        }

        else if(numOutputArguments() < outputVariables().length)
            addOutputArgument(arr);



    }
    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
        super.initFromTensorFlow(nodeDef, initWith, attributesForNode, graph);
        /**
         * name: "MatMul"
         op: "MatMul"
         input: "input"
         input: "Variable/read"
         attr {
         key: "transpose_b"
         value {
         b: false
         }
         }
         attr {
         key: "transpose_a"
         value {
         b: false
         }
         }
         attr {
         key: "T"
         value {
         type: DT_FLOAT
         }
         }

         */

        val isTransposeA = attributesForNode.get("transpose_a").getB();
        val isTransposeB = attributesForNode.get("transpose_b").getB();
        MMulTranspose mMulTranspose = MMulTranspose.builder()
                .transposeA(isTransposeA).transposeB(isTransposeB)
                .build();
        this.mMulTranspose = mMulTranspose;
        val args = args();
    }

    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {
        val isTransposeA = attributesForNode.get("transA").getI() > 0;
        val isTransposeB = attributesForNode.get("transB").getI() > 0;
        MMulTranspose mMulTranspose = MMulTranspose.builder()
                .transposeA(isTransposeA).transposeB(isTransposeB)
                .build();
        this.mMulTranspose = mMulTranspose;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TensorMmul that = (TensorMmul) o;

        if (addedEdges != that.addedEdges) return false;
        if (!Arrays.deepEquals(axes, that.axes)) return false;
        return mMulTranspose != null ? mMulTranspose.equals(that.mMulTranspose) : that.mMulTranspose == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.deepHashCode(axes);
        result = 31 * result + (addedEdges ? 1 : 0);
        result = 31 * result + (mMulTranspose != null ? mMulTranspose.hashCode() : 0);
        return result;
    }


    @Override
    public Op.Type opType() {
        return Op.Type.CUSTOM;
    }

    @Override
    public String onnxName() {
        return "Gemm";
    }

    @Override
    public String tensorflowName() {
        return "matmul";
    }
}
