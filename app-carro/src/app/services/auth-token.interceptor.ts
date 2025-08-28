// src/app/services/auth-token.interceptor.ts
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service'; // Ajuste o caminho se necessário

@Injectable()
export class AuthTokenInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken(); 
    const isLoginRequest = request.url.includes('/login');
    const isApiRequest = request.url.includes('/api/'); // Considera apenas requisições para sua API

    console.log(`%c[Interceptor Debug]%c URL: ${request.url}`, 'color: cyan; font-weight: bold;', 'color: unset;');
    console.log(`%c[Interceptor Debug]%c Token from localStorage: ${token ? 'Found' : 'NOT Found'}`, 'color: cyan; font-weight: bold;', 'color: unset;');
    console.log(`%c[Interceptor Debug]%c Is Login Request: ${isLoginRequest}`, 'color: cyan; font-weight: bold;', 'color: unset;');
    console.log(`%c[Interceptor Debug]%c Is API Request: ${isApiRequest}`, 'color: cyan; font-weight: bold;', 'color: unset;');


    // Só adiciona o token se:
    // 1. Houver um token.
    // 2. NÃO for a requisição de login.
    // 3. For uma requisição para sua API (opcional, mas bom para evitar tokens em recursos estáticos, etc.)
    if (token && !isLoginRequest && isApiRequest) {
      const authReq = request.clone({
        headers: request.headers.set('Authorization', `Bearer ${token}`)
      });
      console.log(`%c[Interceptor Debug]%c Authorization Header ADDED: ${authReq.headers.get('Authorization')}`, 'color: green; font-weight: bold;', 'color: unset;');
      return next.handle(authReq);
    }

    console.log(`%c[Interceptor Debug]%c NO Authorization Header added for ${request.url}. Passing original request.`, 'color: yellow; font-weight: bold;', 'color: unset;');
    return next.handle(request);
  }
}
