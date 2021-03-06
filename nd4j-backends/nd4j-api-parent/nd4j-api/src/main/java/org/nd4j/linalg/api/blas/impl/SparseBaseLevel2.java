package org.nd4j.linalg.api.blas.impl;

import org.nd4j.linalg.api.blas.Level2;
import org.nd4j.linalg.api.blas.params.SparseCOOGemvParameters;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.executioner.DefaultOpExecutioner;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Audrey Loeffel
 */
public abstract class SparseBaseLevel2 extends SparseBaseLevel implements Level2 {


    @Override
    public void gemv(char order, char transA, double alpha, INDArray A, INDArray X, double beta, INDArray Y) {
        checkArgument(A.isMatrix());
        checkArgument(X.isVector());
        checkArgument(Y.isVector());

        SparseCOOGemvParameters parameters = new SparseCOOGemvParameters(A, X, Y);


        switch (A.data().dataType()) {
            case DOUBLE:
                DefaultOpExecutioner.validateDataType(DataBuffer.Type.DOUBLE, parameters.getA(), parameters.getX(),
                                parameters.getY());
                dcoomv(parameters.getAOrdering(), parameters.getM(), parameters.getVal(), parameters.getRowInd(),
                                parameters.getColInd(), parameters.getNnz(), parameters.getX(), parameters.getY());

                break;
            case FLOAT:
                DefaultOpExecutioner.validateDataType(DataBuffer.Type.FLOAT, parameters.getA(), parameters.getX(),
                                parameters.getY());
                scoomv(parameters.getAOrdering(), parameters.getM(), parameters.getVal(), parameters.getRowInd(),
                                parameters.getColInd(), parameters.getNnz(), parameters.getX(), parameters.getY());

                break;
            default:
                throw new UnsupportedOperationException();
        }

    }

    @Override
    public void gemv(char order, char transA, IComplexNumber alpha, IComplexNDArray A, IComplexNDArray X,
                    IComplexNumber beta, IComplexNDArray Y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void gbmv(char order, char TransA, int KL, int KU, double alpha, INDArray A, INDArray X, double beta,
                    INDArray Y) {

    }

    @Override
    public void gbmv(char order, char TransA, int KL, int KU, IComplexNumber alpha, IComplexNDArray A,
                    IComplexNDArray X, IComplexNumber beta, IComplexNDArray Y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void ger(char order, double alpha, INDArray X, INDArray Y, INDArray A) {

    }

    @Override
    public void geru(char order, IComplexNumber alpha, IComplexNDArray X, IComplexNDArray Y, IComplexNDArray A) {

    }

    @Override
    public void hbmv(char order, char Uplo, IComplexNumber alpha, IComplexNDArray A, IComplexNDArray X,
                    IComplexNumber beta, IComplexNDArray Y) {

    }

    @Override
    public void hemv(char order, char Uplo, IComplexNumber alpha, IComplexNDArray A, IComplexNDArray X,
                    IComplexNumber beta, IComplexNDArray Y) {

    }

    @Override
    public void her2(char order, char Uplo, IComplexNumber alpha, IComplexNDArray X, IComplexNDArray Y,
                    IComplexNDArray A) {

    }

    @Override
    public void hpmv(char order, char Uplo, int N, IComplexNumber alpha, IComplexNDArray Ap, IComplexNDArray X,
                    IComplexNumber beta, IComplexNDArray Y) {

    }

    @Override
    public void hpr2(char order, char Uplo, IComplexNumber alpha, IComplexNDArray X, IComplexNDArray Y,
                    IComplexNDArray Ap) {

    }

    @Override
    public void sbmv(char order, char Uplo, double alpha, INDArray A, INDArray X, double beta, INDArray Y) {

    }

    @Override
    public void spmv(char order, char Uplo, double alpha, INDArray Ap, INDArray X, double beta, INDArray Y) {

    }

    @Override
    public void spr(char order, char Uplo, double alpha, INDArray X, INDArray Ap) {

    }

    @Override
    public void spr2(char order, char Uplo, double alpha, INDArray X, INDArray Y, INDArray A) {

    }

    @Override
    public void symv(char order, char Uplo, double alpha, INDArray A, INDArray X, double beta, INDArray Y) {

    }

    @Override
    public void syr(char order, char Uplo, int N, double alpha, INDArray X, INDArray A) {

    }

    @Override
    public void syr2(char order, char Uplo, double alpha, INDArray X, INDArray Y, INDArray A) {

    }

    @Override
    public void tbmv(char order, char Uplo, char TransA, char Diag, INDArray A, INDArray X) {

    }

    @Override
    public void tbsv(char order, char Uplo, char TransA, char Diag, INDArray A, INDArray X) {

    }

    @Override
    public void tpmv(char order, char Uplo, char TransA, char Diag, INDArray Ap, INDArray X) {

    }

    @Override
    public void tpsv(char order, char Uplo, char TransA, char Diag, INDArray Ap, INDArray X) {

    }

    @Override
    public void trmv(char order, char Uplo, char TransA, char Diag, INDArray A, INDArray X) {

    }

    @Override
    public void trsv(char order, char Uplo, char TransA, char Diag, INDArray A, INDArray X) {

    }

    // ----
    protected abstract void scoomv(char transA, int M, DataBuffer values, DataBuffer rowInd, DataBuffer colInd, int nnz,
                    INDArray x, INDArray y);

    protected abstract void dcoomv(char transA, int M, DataBuffer values, DataBuffer rowInd, DataBuffer colInd, int nnz,
                    INDArray x, INDArray y);
}
