# Spring MVC safe characters for URL paths & path variable values

This file summarizes results of [SpringTest](SpringTest.kt).

## Analysis

- Spring can handle the safe characters defined by HTTP-specs,
  **EXCEPT DOT CHARACTER**:

    ```
    abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_~!$'()*,&+=:@
    ```

- Spring can NOT handle dot character as expected, because it gives
  the dot character special meaning and truncates the string starting
  from the last dot character.
  [Suggested solution](https://www.mkyong.com/spring-mvc/spring-mvc-pathvariable-dot-get-truncated/)
  can not resolve all cases, and complicates the code.

- Spring can handle most unsafe characters even if they not encoded:

    ```
    çöğüşÇÖĞÜŞİı"é<>£^#½&§{[]}|@∑€®₺¥üiöπ¨æß∂ƒğ^∆¬´æ`<>|Ω≈√∫~≤≥÷
    ```

- Spring can handle the following unsafe characters only when they are
  encoded, because they change meaning of the URL:

    ```
    ? -> starts query params
    ; -> starts matrix params
    % -> starts percent-encoded pair such as %2F
    ```

- Spring can NOT handle back and forward slashes even if they are encoded,
  and returns `400 BAD REQUEST`. May be an intentional behaviour because
   of security considerations. BTW,
  [An ugly solution](https://stackoverflow.com/a/2335449/471214)
  is possible.

    ```
    /\
    ```


## Conclusions

- **Encode URL's in client code:** For HTTP-spec compatibility,
  you should always encode path segments (and other URL components)
  properly in client-side. This won't break anything in current
  behaviour, but may repair possible problems if one of these characters
  intended to be used in path:

    ```
    ? ; %
    ```

- **Prevent dot** in path & path variables. Request params can be used
  instead.

- **Prevent both forward and back slashes** in path & path variables.
  Request params can be used instead.

- If you prefer request params, you still need client-side encoding for
  some characters such as `&+`.
