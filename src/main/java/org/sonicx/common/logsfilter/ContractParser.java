package org.sonicx.common.logsfilter;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.util.StringUtils;
import org.spongycastle.crypto.OutputLengthException;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.sonicx.common.runtime.utils.MUtil;
import org.sonicx.common.runtime.vm.DataWord;
import org.sonicx.core.Wallet;
import org.sonicx.protos.Protocol.SmartContract.ABI;

@Slf4j(topic = "Parser")
public class ContractParser {

  private static final int DATAWORD_UNIT_SIZE = 32;

  private enum Type {
    UNKNOWN,
    INT_NUMBER,
    BOOL,
    FLOAT_NUMBER,
    FIXED_BYTES,
    ADDRESS,
    STRING,
    BYTES,
  }

  protected static String parseDataBytes(byte[] data, String typeStr, int index) {
    try {
      byte[] startBytes = subBytes(data, index * DATAWORD_UNIT_SIZE, DATAWORD_UNIT_SIZE);
      Type type = basicType(typeStr);

      if (type == Type.INT_NUMBER) {
        return new BigInteger(startBytes).toString();
      } else if (type == Type.BOOL) {
        return String.valueOf(!DataWord.isZero(startBytes));
      } else if (type == Type.FIXED_BYTES) {
        return Hex.toHexString(startBytes);
      } else if (type == Type.ADDRESS) {
        byte[] last20Bytes = Arrays.copyOfRange(startBytes, 12, startBytes.length);
        return Wallet.encode58Check(MUtil.convertToSonicxAddress(last20Bytes));
      } else if (type == Type.STRING || type == Type.BYTES) {
        int start = intValueExact(startBytes);
        byte[] lengthBytes = subBytes(data, start, DATAWORD_UNIT_SIZE);
        // this length is byte count. no need X 32
        int length = intValueExact(lengthBytes);
        byte[] realBytes =
            length > 0 ? subBytes(data, start + DATAWORD_UNIT_SIZE, length) : new byte[0];
        return type == Type.STRING ? new String(realBytes) : Hex.toHexString(realBytes);
      }
    } catch (OutputLengthException | ArithmeticException e) {
      logger.debug("parseDataBytes ", e);
    }
    throw new UnsupportedOperationException("unsupported type:" + typeStr);
  }

/**
   * parse Data into map<String, Object> If parser failed, then return {"0",
   * Hex.toHexString(data)} Only support basic solidity type, String, Bytes. Fixed Array or dynamic
   * Array are not support yet (then return {"0": Hex.toHexString(data)}).
   */
  public static Map<String, String> parseData(byte[] data, ABI.Entry entry) {

    data = Arrays.copyOfRange(data, 4, data.length);
    Map<String, String> map = new HashMap<>();
    if (ArrayUtils.isEmpty(data)) {
      return map;
    }

    // the first is the signature.
    List<ABI.Entry.Param> list = entry.getInputsList();
    Integer startIndex = 0;
    try {
      // this one starts from the first position.
      int index = 0;
      for (Integer i = 0; i < list.size(); ++i) {
        ABI.Entry.Param param = list.get(i);
        if (param.getIndexed()) {
          continue;
        }
        if (startIndex == 0) {
          startIndex = i;
        }

        String str = parseDataBytes(data, param.getType(), index++);
        if (StringUtils.isNotNullOrEmpty(param.getName())) {
          map.put(param.getName(), str);
        }
//        map.put("" + i, str);

      }
      if (list.size() == 0) {
        map.put("0", Hex.toHexString(data));
      }
    } catch (UnsupportedOperationException e) {
      logger.debug("UnsupportedOperationException", e);
      map.clear();
      map.put(startIndex.toString(), Hex.toHexString(data));
    }
    return map;
  }
  // don't support these type yet : bytes32[10][10]  OR  bytes32[][10]
  protected static Type basicType(String type) {
    if (!Pattern.matches("^.*\\[\\d*\\]$", type)) {
      // ignore not valide type such as "int92", "bytes33", these types will be compiled failed.
      if (type.startsWith("int") || type.startsWith("uint") || type.startsWith("srcToken")) {
        return Type.INT_NUMBER;
      } else if ("bool".equals(type)) {
        return Type.BOOL;
      } else if ("address".equals(type)) {
        return Type.ADDRESS;
      } else if (Pattern.matches("^bytes\\d+$", type)) {
        return Type.FIXED_BYTES;
      } else if ("string".equals(type)) {
        return Type.STRING;
      } else if ("bytes".equals(type)) {
        return Type.BYTES;
      }
    }
    return Type.UNKNOWN;
  }

  protected static Integer intValueExact(byte[] data) {
    return new BigInteger(data).intValueExact();
  }

  protected static byte[] subBytes(byte[] src, int start, int length) {
    if (ArrayUtils.isEmpty(src) || start >= src.length || length < 0) {
      throw new OutputLengthException("data start:" + start + ", length:" + length);
    }
    byte[] dst = new byte[length];
    System.arraycopy(src, start, dst, 0, Math.min(length, src.length - start));
    return dst;
  }
}
