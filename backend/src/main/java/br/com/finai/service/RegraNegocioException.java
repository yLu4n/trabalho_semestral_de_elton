package br.com.finai.service;
public class RegraNegocioException extends RuntimeException {
    public final int status;
    public RegraNegocioException(String mensagem) { this(422, mensagem); }
    public RegraNegocioException(int status, String mensagem) { super(mensagem); this.status=status; }
}
