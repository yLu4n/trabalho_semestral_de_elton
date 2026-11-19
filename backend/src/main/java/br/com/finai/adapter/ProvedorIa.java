package br.com.finai.adapter;
import com.fasterxml.jackson.databind.JsonNode;
public interface ProvedorIa {
    JsonNode gerar(String operacao,String instrucoes,Object dados,boolean segundaTentativa);
}
