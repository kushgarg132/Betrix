import { useMutation } from '@apollo/client/react';
import { GOOGLE_LOGIN, GUEST_LOGIN } from '../graphql/mutations';

/**
 * Thin wrappers around the sign-in mutations. Storing the token and loading the user is
 * AuthContext's job; callers hand the returned token to AuthContext's login().
 */
export function useAuth() {
  const [guestLoginMutation] = useMutation(GUEST_LOGIN);
  const [googleLoginMutation] = useMutation(GOOGLE_LOGIN);

  const guestLogin = async () => (await guestLoginMutation()).data.guestLogin;
  const googleLogin = async (idToken) =>
    (await googleLoginMutation({ variables: { idToken } })).data.googleLogin;

  return { guestLogin, googleLogin };
}
