import React, { createContext, useContext, useState, useEffect } from 'react';
import { AuthContext as OAuth2Context, AuthProvider as OAuth2Provider, TAuthConfig } from 'react-oauth2-code-pkce';
import { oauth2Config } from '../config/oauth';

interface AuthContextType {
  isAuthenticated: boolean;
  token: string | null;
  login: () => void;
  logout: () => void;
  user: any;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderWrapperProps {
  children: React.ReactNode;
}

export const AuthProviderWrapper: React.FC<AuthProviderWrapperProps> = ({ children }) => {
  const authConfig: TAuthConfig = {
    clientId: oauth2Config.clientId,
    authorizationEndpoint: oauth2Config.authorizationEndpoint,
    tokenEndpoint: oauth2Config.tokenEndpoint,
    redirectUri: oauth2Config.redirectUri,
    scope: oauth2Config.scope,
    autoLogin: false,
  };

  return (
    <OAuth2Provider authConfig={authConfig}>
      <AuthProviderInternal>{children}</AuthProviderInternal>
    </OAuth2Provider>
  );
};

const AuthProviderInternal: React.FC<AuthProviderWrapperProps> = ({ children }) => {
  const { token, tokenData, login, logOut } = useContext(OAuth2Context);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState<any>(null);

  useEffect(() => {
    if (token) {
      setIsAuthenticated(true);
      // Extract user info from token data if available
      if (tokenData) {
        setUser({
          email: tokenData.email || null,
          name: tokenData.name || null,
        });
      }
    } else {
      setIsAuthenticated(false);
      setUser(null);
    }
  }, [token, tokenData]);

  const handleLogout = () => {
    logOut();
    setIsAuthenticated(false);
    setUser(null);
  };

  const value: AuthContextType = {
    isAuthenticated,
    token,
    login,
    logout: handleLogout,
    user,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
